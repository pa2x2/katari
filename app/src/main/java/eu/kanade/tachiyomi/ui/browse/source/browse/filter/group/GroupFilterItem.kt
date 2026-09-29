package eu.kanade.tachiyomi.ui.browse.source.browse.filter.group

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.change.FilterChanges

/** A group nested inside another group: header, tools, and options drawn together with their own state. */
@Composable
internal fun GroupFilterItem(
    filter: EntryFilter.Group<*>,
    changes: FilterChanges,
    changedOnly: Boolean,
    onUpdate: () -> Unit,
    onReset: (EntryFilter<*>) -> Unit,
    focus: List<Int>?,
    leadingDivider: Boolean,
    item: @Composable (child: EntryFilter<*>, childFocus: List<Int>?) -> Unit,
) {
    val state = rememberSaveable(filter, saver = FilterGroupUiState.Saver) {
        FilterGroupUiState(expanded = focus != null)
    }
    Column {
        GroupFilterHeader(filter, state, changes, changedOnly, leadingDivider)
        if (state.expanded) {
            GroupFilterTools(filter, state, changedOnly, pinned = false, onReset = onReset, onUpdate = onUpdate)
            GroupFilterBody(filter, state, changes, changedOnly, focus, onUpdate, item)
        }
    }
}
