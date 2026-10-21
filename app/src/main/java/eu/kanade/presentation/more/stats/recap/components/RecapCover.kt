package eu.kanade.presentation.more.stats.recap.components

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import eu.kanade.presentation.entry.components.EntryCover
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapTitle
import mihon.presentation.kit.content.CoverFrame
import mihon.presentation.kit.content.storyCoverFrame

/** A title's cover as recap pages show it: lifted off the page, and holdable to hide the title. */
@Composable
internal fun RecapCover(
    title: StatisticsRecapTitle,
    width: Dp,
    modifier: Modifier = Modifier,
) {
    EntryCover.Book(
        data = title.cover,
        contentDescription = title.title,
        frame = CoverFrame.None,
        modifier = modifier
            .recapTitleTarget(title.entryId)
            .width(width)
            .storyCoverFrame(width),
    )
}
