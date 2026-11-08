package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.filter.DetachedEntryFilter
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList

/** The source instance that supplied this projection's options and concrete filter behavior. */
internal interface LegacySourceFilter {
    val legacyFilter: Filter<*>
}

/** Bind captured values to their original instances and restore cached source state after the request. */
suspend fun <T> EntryFilterList.withLegacyFilterValues(block: suspend (FilterList) -> T): T {
    val restores = mutableListOf<() -> Unit>()
    fun bind(value: EntryFilter<*>): Filter<*> {
        val projection = (value as? DetachedEntryFilter)?.sourceFilter ?: value
        val target = (projection as? LegacySourceFilter)?.legacyFilter ?: return value.toLegacyFilter()
        val previous = target.state
        restores += { target.assignLegacyState(previous) }
        val selected = when (value) {
            is EntryFilter.Group<*> -> value.state.filterIsInstance<EntryFilter<*>>().map(::bind)
            is EntryFilter.Sort -> value.state?.let { Filter.Sort.Selection(it.index, it.ascending) }
            else -> value.state
        }
        target.assignLegacyState(selected)
        return target
    }
    return try {
        block(FilterList(map(::bind)))
    } finally {
        restores.asReversed().forEach { it() }
    }
}

@Suppress("UNCHECKED_CAST")
private fun Filter<*>.assignLegacyState(value: Any?) {
    (this as Filter<Any?>).state = value
}
