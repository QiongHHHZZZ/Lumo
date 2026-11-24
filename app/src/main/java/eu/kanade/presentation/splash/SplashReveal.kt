package eu.kanade.presentation.splash

import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import eu.kanade.tachiyomi.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Second half of the launch screen.
 *
 * The system paints a flat colour with the app icon floating in the middle.
 * This overlay takes over on the first frame the app draws: it starts with the
 * icon exactly where the system left it, grows that little square into the whole
 * illustration, and finally fades away to reveal the app.
 *
 * The illusion only works if the starting frame matches the system's frame -
 * same colour, same icon size, same position. If a device lays the icon out
 * differently the seam shows as a jump. [SplashIconSize] below was measured off
 * a real device; it is the knob to turn when the icon pops.
 *
 * @param handoff flips to true once the app is ready to draw its real UI.
 *   Waiting for it means the zoom starts from a still frame instead of being
 *   half over by the time the launch window is gone.
 */
@Composable
fun SplashReveal(
    handoff: State<Boolean>,
    modifier: Modifier = Modifier,
) {
    val zoom = remember { Animatable(0f) }
    val fade = remember { Animatable(1f) }
    var running by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        // Respect the "remove animations" accessibility setting: show nothing
        // at all rather than a stuttering one.
        if (!ValueAnimator.areAnimatorsEnabled()) {
            running = false
            return@LaunchedEffect
        }
        // Never wait forever for a flag some ROM may never set.
        withTimeoutOrNull(HANDOFF_TIMEOUT_MS) {
            snapshotFlow { handoff.value }.first { it }
        }
        // A beat of stillness first: the launch window disappears in one frame,
        // and jumping straight into motion reads as a cut. Then the picture
        // grows and dissolves at the same time, so it never stops dead before
        // the app appears.
        coroutineScope {
            launch {
                delay(HOLD_MS)
                zoom.animateTo(1f, tween(ZOOM_MS, easing = ZoomEasing))
            }
            launch {
                delay(HOLD_MS + (ZOOM_MS * FADE_START).toLong())
                fade.animateTo(0f, tween(FADE_MS, easing = LinearEasing))
            }
        }
        running = false
    }

    if (!running) return

    val context = LocalContext.current
    val art = remember { BitmapFactory.decodeResource(context.resources, R.drawable.splash_art) } ?: return
    val iconSide = with(LocalDensity.current) { SplashIconSize.toPx() }
    val paint = remember { Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG) }
    // One Path reused across all frames (see drawReveal).
    val openingPath = remember { Path() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = fade.value },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawReveal(
                art = art,
                paint = paint,
                openingPath = openingPath,
                iconSide = iconSide,
                t = zoom.value,
            )
        }

        // The name rides in on the tail of the zoom. It cannot be there on the
        // first frame - the launch window only knows how to draw one icon on a
        // flat colour - so appearing later is the only honest option, and the
        // second half of the zoom is where it reads as the picture settling.
        Text(
            text = WORDMARK_TEXT,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = WORDMARK_BOTTOM_INSET)
                .offset(x = WORDMARK_CENTRE_NUDGE)
                .graphicsLayer {
                    alpha = ((zoom.value - WORDMARK_FROM) / WORDMARK_OVER).coerceIn(0f, 1f)
                },
            style = WordmarkStyle,
        )
    }
}

/**
 * Paints the backdrop, the visible slice of the illustration, and a soft rim
 * where the slice meets the backdrop.
 *
 * All of the geometry lives in "illustration space": [viewX], [viewY], [viewW]
 * and [viewH] describe which part of the square picture is shown, and the
 * on-screen opening is sized to show exactly that part. Both ends of the move
 * stay inside the picture, so nothing needs clamping along the way.
 */
private fun DrawScope.drawReveal(
    art: Bitmap,
    paint: Paint,
    openingPath: Path,
    iconSide: Float,
    t: Float,
) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return

    val icon = iconSide.coerceIn(1f, minOf(w, h))
    val endWidth = (w / h).coerceIn(0.2f, 1f)

    // The face is not in the middle of its own square - it sits a little to the
    // left - so ending the zoom on the square's centre parked the face left of
    // centre on screen. The opening is anchored on the face instead.
    val endLeft = (ART_FACE_X - endWidth / 2f).coerceIn(0f, 1f - endWidth)

    // At t = 0 the visible part is exactly the square the icon was cut from; at
    // t = 1 it is the whole height, one screen width across.
    val viewX = lerp(ART_ICON_LEFT, endLeft, t)
    val viewY = lerp(ART_ICON_TOP, 0f, t)
    val viewW = lerp(ART_ICON_SIDE, endWidth, t)
    val viewH = lerp(ART_ICON_SIDE, 1f, t)

    // Height leads and width follows the shape of what is visible, which makes
    // the side gaps close first and leaves a strip above and below, so it never
    // reads as a card floating in the middle.
    val windowH = lerp(icon, h, t)
    val windowW = windowH * (viewW / viewH)
    val windowLeft = (w - windowW) / 2f
    val windowTop = (h - windowH) / 2f

    // The illustration as a whole, sized so the visible part fills the opening.
    val whole = windowH / viewH
    val artLeft = windowLeft - viewX * whole
    val artTop = windowTop - viewY * whole

    drawRect(color = Backdrop, size = size)

    // The icon's rounded corners melt away early, while it is still small.
    // One Path is reused for every frame; allocating a fresh one per frame
    // would churn the garbage collector for the whole ~1.3s animation.
    val corner = icon * ICON_CORNER_RADIUS * (1f - (t / CORNER_FRACTION).coerceIn(0f, 1f))
    openingPath.rewind()
    openingPath.addRoundRect(
        RoundRect(
            left = windowLeft,
            top = windowTop,
            right = windowLeft + windowW,
            bottom = windowTop + windowH,
            cornerRadius = CornerRadius(corner, corner),
        ),
    )

    clipPath(openingPath) {
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawBitmap(
                art,
                android.graphics.Rect(0, 0, art.width, art.height),
                android.graphics.RectF(artLeft, artTop, artLeft + whole, artTop + whole),
                paint,
            )
        }
    }

    // A hard-edged rectangle of picture on a flat field reads as a card sliding
    // over the backdrop rather than as the icon opening out. Fading the picture
    // into the backdrop across a rim fixes that. The rim is zero at both ends on
    // purpose: at the start it has to sit exactly where the launch window's icon
    // was, and at the end the picture covers the screen and has no edge left.
    val feather = windowH * FEATHER_FRACTION * 4f * t * (1f - t)
    if (feather <= 1f) return

    val left = windowLeft
    val right = windowLeft + windowW
    val top = windowTop
    val bottom = windowTop + windowH
    val intoBackdrop = listOf(Backdrop, Color.Transparent)
    val outOfBackdrop = listOf(Color.Transparent, Backdrop)

    drawRect(
        brush = Brush.verticalGradient(intoBackdrop, startY = top, endY = top + feather),
        topLeft = Offset(left, top),
        size = Size(windowW, feather),
    )
    drawRect(
        brush = Brush.verticalGradient(outOfBackdrop, startY = bottom - feather, endY = bottom),
        topLeft = Offset(left, bottom - feather),
        size = Size(windowW, feather),
    )
    drawRect(
        brush = Brush.horizontalGradient(intoBackdrop, startX = left, endX = left + feather),
        topLeft = Offset(left, top),
        size = Size(feather, windowH),
    )
    drawRect(
        brush = Brush.horizontalGradient(outOfBackdrop, startX = right - feather, endX = right),
        topLeft = Offset(right - feather, top),
        size = Size(feather, windowH),
    )
}

/** Size of the icon the launch window draws, measured off a phone screenshot. */
private val SplashIconSize = 128.dp

/** Matches `@color/splash_backdrop`. */
private val Backdrop = Color(0xFFCEC9DF)

// Where the icon sits inside splash_art.webp, as fractions of the square
// (a 990 px window at 150,27 inside the 1536 px image - pixel exact).
private const val ART_ICON_LEFT = 150f / 1536f
private const val ART_ICON_TOP = 27f / 1536f
private const val ART_ICON_SIDE = 990f / 1536f

/** Centre of the face inside the square - what the zoom ends up centred on. */
private const val ART_FACE_X = 0.403f

/** Corner radius of the icon, as a fraction of its side. */
private const val ICON_CORNER_RADIUS = 0.222f

/** Narrowest point of the soft rim, as a fraction of the opening's height. */
private const val FEATHER_FRACTION = 0.11f

/** Portion of the zoom spent rounding the icon off into a rectangle. */
private const val CORNER_FRACTION = 0.45f

private const val HOLD_MS = 130L
private const val ZOOM_MS = 1300
private const val FADE_MS = 420

/** Point in the zoom, 0..1, at which the dissolve into the app begins. */
private const val FADE_START = 0.7f

/** Give up waiting for the hand-off after this long. */
private const val HANDOFF_TIMEOUT_MS = 120L

/**
 * The app name. `app_name` in the i18n module is marked `translatable="false"`
 * and reads "Lumo" everywhere, so there is nothing to localise here.
 */
private const val WORDMARK_TEXT = "Lumo"

/** Point in the zoom, 0..1, at which the name starts to come in. */
private const val WORDMARK_FROM = 0.35f

/** How much of the zoom the name takes to arrive. */
private const val WORDMARK_OVER = 0.45f

/** Distance from the bottom edge to the name. */
private val WORDMARK_BOTTOM_INSET = 88.dp

/**
 * Letter spacing is added after each letter, including the last one, so a
 * centred word sits half a gap to the left. This puts it back.
 */
private val WORDMARK_CENTRE_NUDGE = 4.dp

private val WordmarkStyle = TextStyle(
    fontSize = 26.sp,
    fontWeight = FontWeight.Black,
    letterSpacing = 8.sp,
    color = Color(0xFF3A2B42),
)

/**
 * Opens slowly and settles slowly, with the fast part kept short and in the
 * middle. The picture grows about four times over the run, so an even curve
 * reads as a shove; the long slow start is what makes it feel like the icon
 * opening out instead of snapping to full size.
 */
private val ZoomEasing = CubicBezierEasing(0.5f, 0f, 0.2f, 1f)
