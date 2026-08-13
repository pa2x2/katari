package mihon.entry.interactions.book.reader

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import mihon.entry.interactions.reader.settings.BookDocumentReaderProgressStyle
import tachiyomi.presentation.core.components.reader.ReaderPageIndicator
import tachiyomi.presentation.core.components.reader.ReaderProgressIndicator
import kotlin.math.roundToInt

internal sealed interface BookReaderProgress {
    data class Page(
        val currentPage: Int,
        val totalPages: Int,
    ) : BookReaderProgress

    data class Chapter(
        val value: Float,
        val style: BookDocumentReaderProgressStyle,
        val activeColor: Color,
        val trackColor: Color,
    ) : BookReaderProgress
}

internal val BookReaderProgress.usesFooter: Boolean
    get() = this is BookReaderProgress.Page ||
        (this is BookReaderProgress.Chapter && style == BookDocumentReaderProgressStyle.PERCENTAGE)

@Composable
internal fun BookReaderFooterProgressIndicator(
    progress: BookReaderProgress,
    modifier: Modifier = Modifier,
) {
    when (progress) {
        is BookReaderProgress.Page -> ReaderPageIndicator(
            currentPage = progress.currentPage,
            totalPages = progress.totalPages,
            modifier = modifier,
        )
        is BookReaderProgress.Chapter -> {
            if (progress.style == BookDocumentReaderProgressStyle.PERCENTAGE) {
                ReaderProgressIndicator(
                    text = "${(progress.value.coerceIn(0f, 1f) * 100).roundToInt()}%",
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
internal fun BookReaderAmbientProgressIndicator(
    progress: BookReaderProgress,
    modifier: Modifier = Modifier,
) {
    if (progress !is BookReaderProgress.Chapter || progress.style == BookDocumentReaderProgressStyle.PERCENTAGE) {
        return
    }
    val value = progress.value.coerceIn(0f, 1f)
    Canvas(
        modifier = modifier.semantics {
            progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f)
        },
    ) {
        val edgeInset = 2.dp.toPx()
        val trailingEdgeX = if (layoutDirection == LayoutDirection.Ltr) {
            size.width - edgeInset
        } else {
            edgeInset
        }
        when (progress.style) {
            BookDocumentReaderProgressStyle.EDGE_FILL_RAIL -> {
                drawLine(
                    color = progress.trackColor,
                    start = Offset(trailingEdgeX, 0f),
                    end = Offset(trailingEdgeX, size.height),
                    strokeWidth = 1.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                if (value > 0f) {
                    drawLine(
                        color = progress.activeColor,
                        start = Offset(trailingEdgeX, 0f),
                        end = Offset(trailingEdgeX, size.height * value),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
            BookDocumentReaderProgressStyle.EDGE_POSITION_MARKER -> {
                drawLine(
                    color = progress.trackColor,
                    start = Offset(trailingEdgeX, 0f),
                    end = Offset(trailingEdgeX, size.height),
                    strokeWidth = 1.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                val markerHeight = 32.dp.toPx().coerceAtMost(size.height)
                val markerTop = (size.height * value - markerHeight / 2f)
                    .coerceIn(0f, size.height - markerHeight)
                drawLine(
                    color = progress.activeColor,
                    start = Offset(trailingEdgeX, markerTop),
                    end = Offset(trailingEdgeX, markerTop + markerHeight),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            BookDocumentReaderProgressStyle.BOTTOM_HAIRLINE -> {
                val y = size.height - 1.dp.toPx()
                val completedX = size.width * value
                drawLine(
                    color = progress.trackColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                if (value > 0f) {
                    val startX = if (layoutDirection == LayoutDirection.Ltr) 0f else size.width
                    val endX = if (layoutDirection == LayoutDirection.Ltr) completedX else size.width - completedX
                    drawLine(
                        color = progress.activeColor,
                        start = Offset(startX, y),
                        end = Offset(endX, y),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
            BookDocumentReaderProgressStyle.PERCENTAGE -> Unit
        }
    }
}
