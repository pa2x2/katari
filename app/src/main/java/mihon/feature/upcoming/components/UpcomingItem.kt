package mihon.feature.upcoming.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.entry.components.EntryCover
import eu.kanade.presentation.library.update.labelRes
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.library.update.model.EntryUpdateDecisionReason
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.domain.entry.model.EntryCover as DomainEntryCover

private val UpcomingItemHeight = 96.dp

/** @param notCheckedReason why the next automatic update would leave the entry out, if it would. */
@Composable
fun UpcomingItem(
    upcoming: Entry,
    notCheckedReason: EntryUpdateDecisionReason?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .height(UpcomingItemHeight)
            .padding(
                horizontal = MaterialTheme.padding.medium,
                vertical = MaterialTheme.padding.small,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.large),
    ) {
        EntryCover.Book(
            modifier = Modifier.fillMaxHeight(),
            data = DomainEntryCover(
                entryId = upcoming.id,
                sourceId = upcoming.source,
                isFavorite = upcoming.favorite,
                url = upcoming.thumbnailUrl,
                lastModified = upcoming.coverLastModified,
            ),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = upcoming.title,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (notCheckedReason != null) {
                Text(
                    text = stringResource(MR.strings.upcoming_not_checked, stringResource(notCheckedReason.labelRes)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
