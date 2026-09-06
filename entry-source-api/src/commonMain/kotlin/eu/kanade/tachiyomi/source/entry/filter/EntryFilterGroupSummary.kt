package eu.kanade.tachiyomi.source.entry.filter

import eu.kanade.tachiyomi.source.entry.EntryFilter

/** Optional additional text for a collapsed group; never used to calculate its selected count. */
interface EntryFilterGroupSummary {
    fun selectionSummary(values: List<EntryFilter<*>>): String?
}
