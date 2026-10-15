package eu.kanade.tachiyomi.ui.browse.source.browse.filter.control

import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.source.entry.EntryFilter
import mihon.presentation.kit.list.CheckRow
import mihon.presentation.kit.list.TriStateRow

/** A boolean filter option. */
@Composable
internal fun FilterCheckboxRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    CheckRow(title = label, checked = checked, onCheckedChange = { onToggle() })
}

/** A filter option that cycles between not used, included, and excluded, announcing which one it is. */
@Composable
internal fun FilterTriStateRow(filter: EntryFilter.TriState, onUpdate: () -> Unit) {
    TriStateRow(
        title = filter.name,
        state = filter.triState,
        onStateChange = {
            filter.state = it.entryFilterState
            onUpdate()
        },
    )
}
