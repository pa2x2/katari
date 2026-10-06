package eu.kanade.presentation.entry.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.presentation.more.stats.components.rememberStatisticsDurationFormatter
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.material.SECONDARY_ALPHA
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun EntryChapterHeader(
    enabled: Boolean,
    entryType: EntryType,
    chapterCount: Int?,
    missingChapterCount: Int,
    unreadCount: Int,
    /** Expected time to finish the [unreadCount] chapters; null when there's too little to estimate from. */
    catchUpMillis: Long?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val presentation = entryType.entryTypePresentation()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
    ) {
        Text(
            text = if (chapterCount == null) {
                stringResource(presentation.childListTitle)
            } else {
                pluralStringResource(presentation.childCountPlural, count = chapterCount, chapterCount)
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        if (unreadCount > 0 && catchUpMillis != null) {
            val formatDuration = rememberStatisticsDurationFormatter()
            Text(
                text = stringResource(
                    MR.strings.entry_catch_up_estimate,
                    stringResource(presentation.filterUnconsumedLabel),
                    unreadCount,
                    formatDuration(catchUpMillis),
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        MissingChaptersWarning(missingChapterCount, entryType)
    }
}

@Composable
private fun MissingChaptersWarning(count: Int, entryType: EntryType) {
    if (count == 0) {
        return
    }

    Text(
        text = pluralStringResource(entryType.entryTypePresentation().missingChildCountPlural, count = count, count),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error.copy(alpha = SECONDARY_ALPHA),
    )
}
