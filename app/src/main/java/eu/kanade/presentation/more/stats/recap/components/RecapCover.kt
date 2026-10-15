package eu.kanade.presentation.more.stats.recap.components

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp
import eu.kanade.presentation.entry.components.EntryCover
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapTitle
import mihon.presentation.kit.content.StoryMetrics

/** A title's cover as recap pages show it: lifted off the page, and holdable to hide the title. */
@Composable
internal fun RecapCover(
    title: StatisticsRecapTitle,
    width: Dp,
    modifier: Modifier = Modifier,
) {
    val shape = StoryMetrics.coverShape(width)
    EntryCover.Book(
        data = title.cover,
        shape = shape,
        contentDescription = title.title,
        modifier = modifier
            .recapTitleTarget(title.entryId)
            .width(width)
            .shadow(elevation = StoryMetrics.coverShadow(width), shape = shape),
    )
}
