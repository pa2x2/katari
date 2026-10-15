package mihon.entry.interactions.book.reader

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import mihon.entry.interactions.reader.settings.BookDocumentReaderProgressStyle
import mihon.entry.viewer.chrome.ui.ReaderPageIndicator
import mihon.entry.viewer.chrome.ui.ReaderProgressIndicator
import mihon.presentation.kit.content.ReadingMetrics
import mihon.presentation.kit.theme.KatariTheme
import kotlin.math.roundToInt

internal sealed interface BookReaderProgress {
    data class Page(
        val currentPage: Int,
        val totalPages: Int,
    ) : BookReaderProgress

    data class Chapter(
        val value: FloatState,
        val style: BookDocumentReaderProgressStyle,
        val activeColor: Color,
        val trackColor: Color,
    ) : BookReaderProgress
}

internal data class BookReaderProgressInsets(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    companion object {
        val Zero = BookReaderProgressInsets(0, 0, 0, 0)
    }
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
                    text = "${(progress.value.floatValue.coerceIn(0f, 1f) * 100).roundToInt()}%",
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
internal fun BookReaderAmbientProgressIndicator(
    progress: BookReaderProgress,
    roundedCornerInsets: BookReaderProgressInsets,
    modifier: Modifier = Modifier,
) {
    if (progress !is BookReaderProgress.Chapter || progress.style == BookDocumentReaderProgressStyle.PERCENTAGE) {
        return
    }
    val value = progress.value.floatValue.coerceIn(0f, 1f)
    Canvas(
        modifier = modifier.semantics {
            progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f)
        },
    ) {
        val edgeInset = KatariTheme.spacing.xxs.toPx()
        val trailingEdgeX = if (layoutDirection == LayoutDirection.Ltr) {
            size.width - edgeInset
        } else {
            edgeInset
        }
        val trackTop = roundedCornerInsets.top.toFloat().coerceIn(0f, size.height)
        val trackBottom = (size.height - roundedCornerInsets.bottom).coerceIn(trackTop, size.height)
        val verticalTrackHeight = trackBottom - trackTop
        when (progress.style) {
            BookDocumentReaderProgressStyle.EDGE_FILL_RAIL -> {
                drawLine(
                    color = progress.trackColor,
                    start = Offset(trailingEdgeX, trackTop),
                    end = Offset(trailingEdgeX, trackBottom),
                    strokeWidth = KatariTheme.sizes.border.toPx(),
                    cap = StrokeCap.Round,
                )
                if (value > 0f) {
                    drawLine(
                        color = progress.activeColor,
                        start = Offset(trailingEdgeX, trackTop),
                        end = Offset(trailingEdgeX, trackTop + verticalTrackHeight * value),
                        strokeWidth = ReadingMetrics.progressFillWidth.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
            BookDocumentReaderProgressStyle.EDGE_POSITION_MARKER -> {
                drawLine(
                    color = progress.trackColor,
                    start = Offset(trailingEdgeX, trackTop),
                    end = Offset(trailingEdgeX, trackBottom),
                    strokeWidth = KatariTheme.sizes.border.toPx(),
                    cap = StrokeCap.Round,
                )
                val markerHeight = ReadingMetrics.progressMarkerLength.toPx().coerceAtMost(verticalTrackHeight)
                val markerTop = trackTop + (verticalTrackHeight - markerHeight).coerceAtLeast(0f) * value
                drawLine(
                    color = progress.activeColor,
                    start = Offset(trailingEdgeX, markerTop),
                    end = Offset(trailingEdgeX, markerTop + markerHeight),
                    strokeWidth = ReadingMetrics.progressMarkerWidth.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            BookDocumentReaderProgressStyle.BOTTOM_HAIRLINE -> {
                val y = size.height - KatariTheme.sizes.border.toPx()
                val trackStartX = roundedCornerInsets.left.toFloat().coerceIn(0f, size.width)
                val trackEndX = (size.width - roundedCornerInsets.right).coerceIn(trackStartX, size.width)
                val trackWidth = trackEndX - trackStartX
                drawLine(
                    color = progress.trackColor,
                    start = Offset(trackStartX, y),
                    end = Offset(trackEndX, y),
                    strokeWidth = KatariTheme.sizes.border.toPx(),
                    cap = StrokeCap.Round,
                )
                if (value > 0f) {
                    val startX = if (layoutDirection == LayoutDirection.Ltr) trackStartX else trackEndX
                    val endX = if (layoutDirection == LayoutDirection.Ltr) {
                        trackStartX + trackWidth * value
                    } else {
                        trackEndX - trackWidth * value
                    }
                    drawLine(
                        color = progress.activeColor,
                        start = Offset(startX, y),
                        end = Offset(endX, y),
                        strokeWidth = ReadingMetrics.progressFillWidth.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
            BookDocumentReaderProgressStyle.PERCENTAGE -> Unit
        }
    }
}
