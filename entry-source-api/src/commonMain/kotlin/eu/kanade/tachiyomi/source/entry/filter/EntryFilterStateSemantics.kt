package eu.kanade.tachiyomi.source.entry.filter

import eu.kanade.tachiyomi.source.entry.EntryFilter

/** Optional semantics for nonstandard values. Read/write only the supplied projection, never receiver state. */
interface EntryFilterStateSemantics {
    fun activeSelectionCount(value: EntryFilter<*>): Int?
    val canClearSelection: Boolean
    fun clearSelection(value: EntryFilter<*>)
}
