package eu.kanade.presentation.history.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.KeyboardArrowDown
import app.katari.icons.materialsymbols.rounded.KeyboardArrowUp
import app.katari.presentation.kit.button.InlineIconButton
import app.katari.presentation.kit.content.CoverFrame
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.list.CoverRow
import app.katari.presentation.kit.list.RowGroupSeparator
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import eu.kanade.presentation.entry.InlineEntryTypeIndicator
import eu.kanade.presentation.entry.components.EntryCover
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.tachiyomi.ui.history.activity.HistoryActivityDay
import eu.kanade.tachiyomi.ui.history.activity.HistoryActivityDayTitle
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/** The titles of a day in a period's list, most time first; [unfoldedTitles] holds the keys of those unfolded. */
@Composable
internal fun ActivityDayTitles(
    day: HistoryActivityDay,
    unfoldedTitles: Set<String>,
    formatDuration: (Long) -> String,
    onEntryClick: (Long) -> Unit,
    onToggleUnfolded: (String) -> Unit,
) {
    Column {
        day.titles.forEachIndexed { index, title ->
            val key = "${day.localDate}/${title.entryId}"
            if (index > 0) RowGroupSeparator()
            ActivityDayTitleRow(
                title = title,
                unfolded = key in unfoldedTitles,
                formatDuration = formatDuration,
                onClick = { onEntryClick(title.entryId) },
                onToggleUnfolded = { onToggleUnfolded(key) },
            )
        }
    }
}

/**
 * A title within a day of a period's list: its cover, name, what it read and the day's time. Several chapters or
 * episodes are counted, and [unfolded] lists them under it; a lone one is named only when its name isn't the title's.
 */
@Composable
internal fun ActivityDayTitleRow(
    title: HistoryActivityDayTitle,
    unfolded: Boolean,
    formatDuration: (Long) -> String,
    onClick: () -> Unit,
    onToggleUnfolded: () -> Unit,
) {
    val presentation = title.entryType.entryTypePresentation()
    val foldable = title.parts.size > 1
    Column {
        CoverRow(
            cover = {
                EntryCover.Book(
                    data = title.coverData,
                    contentDescription = "",
                    modifier = Modifier.fillMaxSize(),
                    frame = CoverFrame.Thumbnail,
                )
            },
            onClick = onClick,
            trailing = { Text(text = formatDuration(title.durationMillis), role = TextRole.Label, maxLines = 1) },
        ) {
            Text(
                text = title.entryTitle,
                role = TextRole.ItemTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                title.entryType.InlineEntryTypeIndicator()
                Text(
                    text = when {
                        foldable -> pluralStringResource(
                            presentation.childCountPlural,
                            title.parts.size,
                            title.parts.size,
                        )
                        else -> title.parts.single().title?.takeIf { it != title.entryTitle }
                            ?: stringResource(presentation.displayNameLabel)
                    },
                    role = TextRole.Supporting,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (foldable) {
                    InlineIconButton(
                        imageVector = if (unfolded) {
                            MaterialSymbols.Rounded.KeyboardArrowUp
                        } else {
                            MaterialSymbols.Rounded.KeyboardArrowDown
                        },
                        contentDescription = stringResource(
                            if (unfolded) MR.strings.action_collapse else MR.strings.action_expand,
                        ),
                        role = TextRole.Supporting,
                        onClick = onToggleUnfolded,
                    )
                }
            }
        }
        if (unfolded) {
            title.parts.forEach { ActivityPartRow(it, formatDuration, underCover = true) }
        }
    }
}
