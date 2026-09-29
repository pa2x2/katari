package eu.kanade.tachiyomi.ui.browse.source.browse.filter.control

import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.source.entry.EntryFilter
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The user-facing state of an on/off or include/exclude option. */
internal enum class FilterOptionState(val triStateValue: Int) {
    NotUsed(EntryFilter.TriState.STATE_IGNORE),
    Included(EntryFilter.TriState.STATE_INCLUDE),
    Excluded(EntryFilter.TriState.STATE_EXCLUDE),
    ;

    fun next(): FilterOptionState = when (this) {
        NotUsed -> Included
        Included -> Excluded
        Excluded -> NotUsed
    }
}

internal val EntryFilter.TriState.optionState: FilterOptionState
    get() = when (state) {
        EntryFilter.TriState.STATE_INCLUDE -> FilterOptionState.Included
        EntryFilter.TriState.STATE_EXCLUDE -> FilterOptionState.Excluded
        else -> FilterOptionState.NotUsed
    }

@Composable
internal fun FilterOptionState.description(): String = stringResource(
    when (this) {
        FilterOptionState.NotUsed -> MR.strings.tristate_not_used
        FilterOptionState.Included -> MR.strings.tristate_included
        FilterOptionState.Excluded -> MR.strings.tristate_excluded
    },
)
