package mihon.feature.upcoming.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.presentation.entry.components.EntryCover
import eu.kanade.presentation.library.update.labelRes
import mihon.presentation.kit.content.CoverFrame
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.CoverRow
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.TextRole
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.library.update.model.EntryUpdateDecisionReason
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.domain.entry.model.EntryCover as DomainEntryCover

/** @param notCheckedReason why the next automatic update would leave the entry out, if it would. */
@Composable
fun UpcomingItem(
    upcoming: Entry,
    notCheckedReason: EntryUpdateDecisionReason?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CoverRow(
        cover = {
            EntryCover.Book(
                modifier = Modifier.fillMaxSize(),
                frame = CoverFrame.Thumbnail,
                data = DomainEntryCover(
                    entryId = upcoming.id,
                    sourceId = upcoming.source,
                    isFavorite = upcoming.favorite,
                    url = upcoming.thumbnailUrl,
                    lastModified = upcoming.coverLastModified,
                ),
            )
        },
        modifier = modifier,
        onClick = onClick,
    ) {
        Text(
            text = upcoming.title,
            role = TextRole.ItemTitle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (notCheckedReason != null) {
            Text(
                text = stringResource(MR.strings.upcoming_not_checked, stringResource(notCheckedReason.labelRes)),
                role = TextRole.Supporting,
                tone = ContentTone.Error,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
