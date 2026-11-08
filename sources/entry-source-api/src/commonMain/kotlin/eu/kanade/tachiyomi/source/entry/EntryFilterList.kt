package eu.kanade.tachiyomi.source.entry

import androidx.compose.runtime.Stable

/**
 * Current ordered search filters for a source.
 *
 * @property list filters in display order.
 */
@Stable
data class EntryFilterList(val list: List<EntryFilter<*>>) : List<EntryFilter<*>> by list {
    constructor(vararg fs: EntryFilter<*>) : this(if (fs.isNotEmpty()) fs.asList() else emptyList())

    /**
     * Filter state is mutated in place by source filter UIs. Treat every republished list as changed so observable
     * state holders do not suppress those updates.
     */
    override fun equals(other: Any?): Boolean {
        return false
    }

    /** Returns a hash derived from the delegated filter list. */
    override fun hashCode(): Int {
        return list.hashCode()
    }
}
