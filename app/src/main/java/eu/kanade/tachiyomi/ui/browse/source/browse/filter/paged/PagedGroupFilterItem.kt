package eu.kanade.tachiyomi.ui.browse.source.browse.filter.paged

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.change.FilterChangeBadges
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.change.FilterChanges
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The sheet row that opens a paged list, drawn like a group row. */
@Composable
internal fun PagedGroupSummaryItem(filter: EntryFilter.PagedGroup<*>, changes: FilterChanges, onClick: () -> Unit) {
    val selected = filter.currentSelectedItemCount()
    val spacing = KatariTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = spacing.rowInset, end = spacing.m, top = spacing.m, bottom = spacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s),
    ) {
        Column(Modifier.weight(1f)) {
            Text(filter.name, role = TextRole.Label)
            if (selected > 0) {
                Text(
                    text = stringResource(MR.strings.browse_filter_selected_count, selected),
                    role = TextRole.Supporting,
                    tone = ContentTone.Primary,
                )
            }
        }
        FilterChangeBadges(changes[filter], hasIssues = false)
    }
}
