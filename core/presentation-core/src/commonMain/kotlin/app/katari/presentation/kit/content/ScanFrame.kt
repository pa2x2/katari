package app.katari.presentation.kit.content

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole

/** What a [ScanFrame] shows while it has no camera picture: why, and the way to get one where there is a way. */
data class ScanFrameNote(val text: String, val actionLabel: String? = null, val onAction: () -> Unit = {})

/**
 * A square camera [picture] with the frame a code is held in while it is scanned, such as a QR code another device
 * shows, inset among a screen's cards like them. The picture fills the square over a dark backdrop, which shows until
 * the camera starts. While [note] is set it stands in the picture's place, such as a camera that still needs
 * permission. The frame is drawn over either in light lines, which read on any picture.
 */
@Composable
fun ScanFrame(modifier: Modifier = Modifier, note: ScanFrameNote? = null, picture: @Composable () -> Unit = {}) {
    Box(
        modifier = modifier
            .padding(horizontal = KatariTheme.spacing.m)
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(KatariTheme.shapes.large)
            .background(FRAME_BACKDROP),
    ) {
        if (note != null) PictureNote(note) else picture()
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

@Composable
private fun PictureNote(note: ScanFrameNote) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(KatariTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(note.text, role = TextRole.Body, tone = ContentTone.OnMedia)
        note.actionLabel?.let { Button(it, note.onAction) }
    }
}

private const val FRAME_INSET = 0.15f
private val FRAME_STROKE = 3.dp
private val FRAME_CORNER = 16.dp
private val FRAME_COLOR = Color(0xE6FFFFFF)
private val FRAME_BACKDROP = Color(0xFF000000)
