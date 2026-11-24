package eu.kanade.presentation.reader

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import app.luma.di.appGraph
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.util.rememberIs24HourSystem
import eu.kanade.tachiyomi.util.lang.toTimestampString
import kotlinx.coroutines.delay
import tachiyomi.presentation.core.util.collectAsState
import kotlin.time.Clock

/**
 * The clock on the left and the battery on the right, drawn in the band the system status bar
 * normally occupies.
 *
 * That band cannot be measured with [WindowInsets.statusBars]: while reading the bars are hidden and
 * it reports zero, which would leave the text jammed against the very top of the screen. This is why
 * Mihon's reader only ever insets the page by the display cutout — it never needs the band. Drawing
 * into the band does, so the height comes from `statusBarsIgnoringVisibility`, which reports the
 * height the bar would have. The row is given exactly that height and centres its text in it, so the
 * clock and battery land where the system's own clock and battery would be, not below them.
 *
 * The cutout is still honoured, but only on the sides: on a notched phone the notch sits between the
 * clock and the battery, so avoiding it vertically would push both out of the band.
 */
@Composable
fun ReaderStatusBar(
    showClock: Boolean,
    showBattery: Boolean,
    modifier: Modifier = Modifier,
) {
    val clock = if (showClock) rememberClockText() else null
    val level = if (showBattery) rememberBatteryLevel() else null
    if (clock == null && level == null) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(statusBarBandHeight())
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // A box keeps the battery on the right when only one of the two is switched on.
        Box {
            clock?.let { ReaderOutlinedText(text = it) }
        }

        if (level != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BatteryIcon(level = level)
                Spacer(Modifier.width(4.dp))
                ReaderOutlinedText(text = "$level%")
            }
        }
    }
}

/**
 * How tall the status bar band is, even while it is hidden. Falls back to the cutout and then to a
 * minimum, since a device that reports neither would otherwise collapse the row to nothing.
 */
@Composable
private fun statusBarBandHeight(): Dp {
    val statusBar = WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding()
    if (statusBar > 0.dp) return statusBar

    val cutout = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    return cutout.coerceAtLeast(MIN_BAND_HEIGHT)
}

/**
 * A battery outline filled to [level] percent, drawn to match the text: a light fill over a dark
 * halo so it reads on both a white and a black page. Without it the bare percentage reads as any
 * other number.
 */
@Composable
private fun BatteryIcon(
    level: Int,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(width = 16.dp, height = 9.dp)) {
        val stroke = 1.dp.toPx()
        val halo = 1.2.dp.toPx()
        val nubWidth = 1.5.dp.toPx()
        val gap = 1.dp.toPx()
        val bodyWidth = size.width - nubWidth - gap
        val radius = CornerRadius(2.dp.toPx())
        val nubHeight = size.height * 0.42f
        val nubTop = (size.height - nubHeight) / 2f
        val nubCorner = CornerRadius(radius.x / 2f)

        // Halo first: the same outline, fattened and darkened, so the icon survives a light page.
        drawRoundRect(
            color = ReaderOverlayHaloColor,
            size = Size(bodyWidth, size.height),
            cornerRadius = radius,
            style = Stroke(width = stroke + 2 * halo),
        )
        drawRoundRect(
            color = ReaderOverlayHaloColor,
            topLeft = Offset(bodyWidth + gap, nubTop),
            size = Size(nubWidth, nubHeight),
            cornerRadius = nubCorner,
            style = Stroke(width = stroke + 2 * halo),
        )

        drawRoundRect(
            color = ReaderOverlayTextColor,
            size = Size(bodyWidth, size.height),
            cornerRadius = radius,
            style = Stroke(width = stroke),
        )
        drawRoundRect(
            color = ReaderOverlayTextColor,
            topLeft = Offset(bodyWidth + gap, nubTop),
            size = Size(nubWidth, nubHeight),
            cornerRadius = nubCorner,
            style = Stroke(width = stroke),
        )

        val inset = stroke * 2f
        val filledWidth = (bodyWidth - inset * 2) * level.coerceIn(0, 100) / 100f
        val filledHeight = size.height - inset * 2
        if (filledWidth > 0f && filledHeight > 0f) {
            drawRoundRect(
                color = ReaderOverlayTextColor,
                topLeft = Offset(inset, inset),
                size = Size(filledWidth, filledHeight),
                cornerRadius = CornerRadius(radius.x / 2f),
            )
        }
    }
}

/**
 * The current time in the app's own time format, refreshed on every minute boundary. Going by the
 * minute keeps the shown minute from ever being stale, and a format or timezone change is picked up
 * on the next tick.
 */
@Composable
private fun rememberClockText(): String {
    val context = LocalContext.current
    val locale = LocalLocale.current
    val is24HourSystem = rememberIs24HourSystem()
    val timeFormat by remember { context.appGraph.uiPreferences.timeFormat }.collectAsState()
    val formatter = remember(timeFormat, locale, is24HourSystem) {
        UiPreferences.timeFormat(timeFormat, locale.platformLocale, is24HourSystem)
    }

    val text by produceState(Clock.System.now().toTimestampString(formatter), formatter) {
        while (true) {
            value = Clock.System.now().toTimestampString(formatter)
            delay(millisUntilNextMinute())
        }
    }
    return text
}

private fun millisUntilNextMinute(): Long {
    val now = Clock.System.now().toEpochMilliseconds()
    return MINUTE_MILLIS - now % MINUTE_MILLIS + 100
}

/**
 * The battery percentage, kept current from the system's battery broadcast. Null until a reading
 * arrives, so a device that never sends one draws nothing rather than "0%".
 */
@Composable
private fun rememberBatteryLevel(): Int? {
    val context = LocalContext.current
    var level by remember { mutableStateOf<Int?>(null) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                levelOf(intent)?.let { level = it }
            }
        }
        // Registering also hands back the last broadcast, which is what gives us a reading before
        // the first update arrives.
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )?.let { levelOf(it)?.let { current -> level = current } }

        onDispose { context.unregisterReceiver(receiver) }
    }

    return level
}

private fun levelOf(intent: Intent?): Int? {
    if (intent == null) return null
    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    if (level < 0 || scale <= 0) return null
    return level * 100 / scale
}

/** Used when a device reports neither a status bar nor a cutout, so the row does not collapse. */
private val MIN_BAND_HEIGHT = 24.dp

private const val MINUTE_MILLIS = 60_000L
