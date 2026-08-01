package eu.kanade.tachiyomi.ui.browse.source.browse.filter

import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.source.entry.EntryFilter
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.NavigationItem
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun PagedGroupSummaryItem(
    filter: EntryFilter.PagedGroup<*>,
    onClick: () -> Unit,
) {
    NavigationItem(
        label = filter.name,
        subtitle = stringResource(
            MR.strings.browse_filter_selected_count,
            filter.currentSelectedItemCount(),
        ),
        onClick = onClick,
    )
}
