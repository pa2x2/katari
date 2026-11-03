package mihon.presentation.kit.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/**
 * Text drawn straight over pages or video with no backing, outlined in the media background so it reads on any image,
 * such as a reader's page number.
 */
@Composable
fun OutlinedMediaText(
    text: String,
    modifier: Modifier = Modifier,
    role: TextRole = TextRole.Badge,
) {
    val fillColor = KatariTheme.colors.onMedia
    val outlineColor = KatariTheme.colors.media
    var textLayoutResult by remember(text) { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        text = text,
        role = role,
        tone = ContentTone.OnMedia,
        onTextLayout = { textLayoutResult = it },
        modifier = modifier.drawWithContent {
            val layoutResult = textLayoutResult
            if (layoutResult == null) {
                drawContent()
                return@drawWithContent
            }
            layoutResult.multiParagraph.paint(
                canvas = drawContext.canvas,
                color = outlineColor,
                drawStyle = Stroke(width = OUTLINE_WIDTH_PX),
            )
            layoutResult.multiParagraph.paint(canvas = drawContext.canvas, color = fillColor, drawStyle = Fill)
        },
    )
}

private const val OUTLINE_WIDTH_PX = 4f
