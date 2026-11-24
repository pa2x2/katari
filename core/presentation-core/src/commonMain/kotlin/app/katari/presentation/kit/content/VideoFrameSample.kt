package app.katari.presentation.kit.content

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.window.currentWindowLayout

/**
 * A drawn scene in a 16:9 video frame, standing in for a video while something shown over video is set, such as how
 * subtitles look. [content] is laid over the whole frame. The frame is as wide as it may be, but no taller than leaves
 * room for the controls beside it, less in a short window; it is then centred.
 */
@Composable
fun VideoFrameSample(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val maxHeight = if (currentWindowLayout().isShort) SHORT_WINDOW_MAX_HEIGHT else MAX_HEIGHT
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .heightIn(max = maxHeight)
                .aspectRatio(VIDEO_ASPECT_RATIO)
                .clip(KatariTheme.shapes.medium)
                .drawBehind { drawScene() },
            content = content,
        )
    }
}

// A sun setting behind two ridges of hills: light and dark parts, so text over it is seen against both
private fun DrawScope.drawScene() {
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(listOf(SKY_TOP, SKY_HORIZON), endY = h * 0.7f))
    drawCircle(SUN, radius = h * 0.13f, center = Offset(w * 0.66f, h * 0.56f))
    drawPath(
        Path().apply {
            moveTo(0f, h * 0.62f)
            quadraticTo(w * 0.18f, h * 0.48f, w * 0.38f, h * 0.6f)
            quadraticTo(w * 0.6f, h * 0.72f, w * 0.8f, h * 0.56f)
            quadraticTo(w * 0.92f, h * 0.48f, w, h * 0.54f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        },
        FAR_HILLS,
    )
    drawPath(
        Path().apply {
            moveTo(0f, h * 0.8f)
            quadraticTo(w * 0.3f, h * 0.64f, w * 0.56f, h * 0.76f)
            quadraticTo(w * 0.8f, h * 0.86f, w, h * 0.72f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        },
        NEAR_HILLS,
    )
}

private const val VIDEO_ASPECT_RATIO = 16f / 9f
private val MAX_HEIGHT = 240.dp
private val SHORT_WINDOW_MAX_HEIGHT = 128.dp

// The scene keeps its colours in light and dark themes alike, as a video would
private val SKY_TOP = Color(0xFF2C3463)
private val SKY_HORIZON = Color(0xFFE59A72)
private val SUN = Color(0xFFFFD991)
private val FAR_HILLS = Color(0xFF7A5679)
private val NEAR_HILLS = Color(0xFF2A2340)
