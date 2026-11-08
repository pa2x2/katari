package app.katari.presentation.kit.control

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
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
