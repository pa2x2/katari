package mihon.entry.viewer.chrome.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

@Composable
fun ReaderPageIndicator(
    currentPage: Int,
    totalPages: Int,
    modifier: Modifier = Modifier,
) {
    if (currentPage <= 0 || totalPages <= 0) return

    ReaderProgressIndicator(
        text = "$currentPage / $totalPages",
        modifier = modifier,
    )
}

/** Progress text drawn straight over pages, outlined in the media background so it reads on any page. */
@Composable
fun ReaderProgressIndicator(
    text: String,
    modifier: Modifier = Modifier,
) {
    if (text.isBlank()) return

    val fillColor = KatariTheme.colors.onMedia
    val outlineColor = KatariTheme.colors.media
    var textLayoutResult by remember(text) { mutableStateOf<TextLayoutResult?>(null) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        Text(
            text = text,
            role = TextRole.Badge,
            tone = ContentTone.OnMedia,
            onTextLayout = { textLayoutResult = it },
            modifier = Modifier.drawWithContent {
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
                layoutResult.multiParagraph.paint(
                    canvas = drawContext.canvas,
                    color = fillColor,
                    drawStyle = Fill,
                )
            },
        )
    }
}

private const val OUTLINE_WIDTH_PX = 4f
