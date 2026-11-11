package app.katari.presentation.kit.control

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import app.katari.presentation.kit.content.LegendSwatch
import app.katari.presentation.kit.theme.KatariTheme

/**
 * Which source marked a part of a video, so marks from different sources look apart on a [MediaTimeline] and beside
 * their names, such as the intro and credits times two online services found.
 */
enum class MediaMarkSource { First, Second }

/** A part of the video from [start] to [end], fractions of the whole, that [source] marked. */
@Immutable
data class MediaMarkedSpan(val start: Float, val end: Float, val source: MediaMarkSource)

/** The dot beside a mark source's name, in the colour its spans take on a [MediaTimeline]. */
@Composable
fun MediaMarkSwatch(source: MediaMarkSource, modifier: Modifier = Modifier) {
    LegendSwatch(color = KatariTheme.colors.mediaMarks[source.ordinal], modifier = modifier)
}

/**
 * A still bar the length of a video that shows only the [spans] its sources marked, such as the intro and credits times
 * a service has for an episode, before they are put to use on the [MediaTimeline].
 */
@Composable
fun MediaMarksLine(spans: List<MediaMarkedSpan>, modifier: Modifier = Modifier) {
    val colors = KatariTheme.colors
    val markColors = colors.mediaMarks
    Canvas(
        modifier
            .fillMaxWidth()
            .height(MARKS_LINE_HEIGHT)
            .clip(KatariTheme.shapes.full)
            .background(colors.surfaceContainerHighest),
    ) {
        spans.forEach { span ->
            val left = size.width * span.start.coerceIn(0f, 1f)
            // A few seconds of a long film would be too thin to see.
            val width = (size.width * span.end.coerceIn(0f, 1f) - left).coerceAtLeast(size.height)
            drawRect(markColors[span.source.ordinal], Offset(left, 0f), Size(width, size.height))
        }
    }
}

private val MARKS_LINE_HEIGHT = 6.dp
