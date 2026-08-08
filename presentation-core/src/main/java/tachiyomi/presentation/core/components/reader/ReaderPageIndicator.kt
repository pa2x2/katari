package tachiyomi.presentation.core.components.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

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

@Composable
fun ReaderProgressIndicator(
    text: String,
    modifier: Modifier = Modifier,
) {
    if (text.isBlank()) return

    val style = TextStyle(
        color = Color(235, 235, 235),
        fontSize = MaterialTheme.typography.bodySmall.fontSize,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
    )
    val strokeStyle = style.copy(
        color = Color(45, 45, 45),
        drawStyle = Stroke(width = 4f),
    )
    var textLayoutResult by remember(text) { mutableStateOf<TextLayoutResult?>(null) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = style,
            onTextLayout = { textLayoutResult = it },
            modifier = Modifier.drawWithContent {
                val layoutResult = textLayoutResult
                if (layoutResult == null) {
                    drawContent()
                    return@drawWithContent
                }
                layoutResult.multiParagraph.paint(
                    canvas = drawContext.canvas,
                    color = strokeStyle.color,
                    drawStyle = strokeStyle.drawStyle,
                )
                layoutResult.multiParagraph.paint(
                    canvas = drawContext.canvas,
                    color = style.color,
                    drawStyle = Fill,
                )
            },
        )
    }
}
