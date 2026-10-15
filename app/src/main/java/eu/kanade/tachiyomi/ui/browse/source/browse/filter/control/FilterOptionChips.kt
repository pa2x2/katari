package eu.kanade.tachiyomi.ui.browse.source.browse.filter.control

import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.source.entry.EntryFilter
import mihon.presentation.kit.chip.TriStateChip
import tachiyomi.core.common.preference.TriState
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * An on/off or include/exclude option drawn as a chip.
 *
 * Tapping a tri-state option cycles not used → included → excluded; long-pressing excludes it directly.
 */
@Composable
internal fun FilterOptionChip(filter: EntryFilter<*>, onUpdate: () -> Unit) {
    when (filter) {
        is EntryFilter.CheckBox -> TriStateChip(
            label = filter.name,
            state = if (filter.state) TriState.ENABLED_IS else TriState.DISABLED,
            onClick = {
                filter.state = !filter.state
                onUpdate()
            },
        )
        is EntryFilter.TriState -> TriStateChip(
            label = filter.name,
            state = filter.triState,
            onClick = {
                filter.state = filter.triState.next().entryFilterState
                onUpdate()
            },
            onLongClick = {
                filter.state = EntryFilter.TriState.STATE_EXCLUDE
                onUpdate()
            },
            onLongClickLabel = stringResource(MR.strings.filter_exclude),
        )
        else -> Unit
    }
}
