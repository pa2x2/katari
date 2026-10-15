package eu.kanade.tachiyomi.ui.browse.source.browse.filter.control

import eu.kanade.tachiyomi.source.entry.EntryFilter
import tachiyomi.core.common.preference.TriState

/** A source's include/exclude option as the kit's tri-state, which its rows and chips draw. */
internal val EntryFilter.TriState.triState: TriState
    get() = when (state) {
        EntryFilter.TriState.STATE_INCLUDE -> TriState.ENABLED_IS
        EntryFilter.TriState.STATE_EXCLUDE -> TriState.ENABLED_NOT
        else -> TriState.DISABLED
    }

/** The source's state value for a kit tri-state. */
internal val TriState.entryFilterState: Int
    get() = when (this) {
        TriState.DISABLED -> EntryFilter.TriState.STATE_IGNORE
        TriState.ENABLED_IS -> EntryFilter.TriState.STATE_INCLUDE
        TriState.ENABLED_NOT -> EntryFilter.TriState.STATE_EXCLUDE
    }
