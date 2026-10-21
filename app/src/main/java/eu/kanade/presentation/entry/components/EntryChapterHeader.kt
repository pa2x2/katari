package eu.kanade.presentation.entry.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.presentation.more.stats.components.rememberStatisticsDurationFormatter
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
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
            // The same space below as a row group header leaves above the card it names.
            .padding(
                start = KatariTheme.spacing.contentInset,
                end = KatariTheme.spacing.contentInset,
                top = KatariTheme.spacing.xs,
                bottom = KatariTheme.spacing.s,
            ),
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
    ) {
        Text(
            text = if (chapterCount == null) {
                stringResource(presentation.childListTitle)
            } else {
                pluralStringResource(presentation.childCountPlural, count = chapterCount, chapterCount)
            },
            role = TextRole.SectionTitle,
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
                role = TextRole.Supporting,
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
        role = TextRole.Supporting,
        tone = ContentTone.Error,
    )
}
