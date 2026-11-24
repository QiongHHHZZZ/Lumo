package eu.kanade.presentation.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** What the reading overlays (page number, clock, battery) write their text in. */
internal val ReaderOverlayTextColor = Color(235, 235, 235)

/** The halo behind [ReaderOverlayTextColor], so the text reads on a light page too. */
internal val ReaderOverlayHaloColor = Color(45, 45, 45)

private const val HALO_WIDTH = 4f

@Composable
internal fun readerOverlayTextStyle(): TextStyle = TextStyle(
    color = ReaderOverlayTextColor,
    fontSize = MaterialTheme.typography.bodySmall.fontSize,
    fontWeight = FontWeight.Bold,
    letterSpacing = 1.sp,
)

/**
 * [text] drawn in the style every reading overlay uses: a light fill with a dark halo, so it stays
 * readable on both a white and a black page without a background box.
 */
@Composable
internal fun ReaderOutlinedText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = readerOverlayTextStyle(),
) {
    val haloStyle = style.copy(color = ReaderOverlayHaloColor, drawStyle = Stroke(width = HALO_WIDTH))
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Text(text = text, style = haloStyle)
        Text(text = text, style = style)
    }
}
