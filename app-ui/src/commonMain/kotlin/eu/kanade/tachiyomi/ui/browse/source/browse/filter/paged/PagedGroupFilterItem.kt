package eu.kanade.tachiyomi.ui.browse.source.browse.filter.paged

import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.change.FilterChangeBadges
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.change.FilterChanges
import mihon.presentation.kit.content.TonedSpan
import mihon.presentation.kit.list.CollapsibleRowGroupSubheader
import mihon.presentation.kit.theme.ContentTone
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The sheet row that opens a paged list, drawn like a group's header. */
@Composable
internal fun PagedGroupSummaryItem(filter: EntryFilter.PagedGroup<*>, changes: FilterChanges, onClick: () -> Unit) {
    val selected = filter.currentSelectedItemCount()
    CollapsibleRowGroupSubheader(
        title = filter.name,
        summary = listOfNotNull(
            TonedSpan(
                text = stringResource(MR.strings.browse_filter_selected_count, selected),
                tone = ContentTone.Primary,
            ).takeIf { selected > 0 },
        ),
        expanded = false,
        onClick = onClick,
        opensPage = true,
        actions = { FilterChangeBadges(changes[filter], hasIssues = false) },
    )
}
