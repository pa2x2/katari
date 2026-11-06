package mihon.presentation.kit.content

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import mihon.presentation.kit.theme.KatariTheme

/**
 * A square camera [picture] with the frame a code is held in while it is scanned, such as a QR code another device
 * shows. The picture fills the square over a dark backdrop, which shows until the camera starts or stands behind a
 * note in its place written in [mihon.presentation.kit.theme.ContentTone.OnMedia]; the frame is drawn over it in light
 * lines, which read on any picture.
 */
@Composable
fun ScanFrame(modifier: Modifier = Modifier, picture: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(KatariTheme.shapes.large)
            .background(FRAME_BACKDROP),
    ) {
        picture()
        Canvas(Modifier.fillMaxSize()) {
            val inset = size.minDimension * FRAME_INSET
            val stroke = FRAME_STROKE.toPx()
            drawRoundRect(
                color = FRAME_COLOR,
                topLeft = Offset(inset, inset),
                size = Size(size.width - 2 * inset, size.height - 2 * inset),
                cornerRadius = CornerRadius(FRAME_CORNER.toPx()),
                style = Stroke(width = stroke),
            )
        }
    }
}

private const val FRAME_INSET = 0.15f
private val FRAME_STROKE = 3.dp
private val FRAME_CORNER = 16.dp
private val FRAME_COLOR = Color(0xE6FFFFFF)
private val FRAME_BACKDROP = Color(0xFF000000)
