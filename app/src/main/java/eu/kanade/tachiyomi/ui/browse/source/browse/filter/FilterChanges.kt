package eu.kanade.tachiyomi.ui.browse.source.browse.filter

import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.filter.sourceStateSemantics
import java.util.IdentityHashMap

/**
 * How far one filter has moved from the source's default value.
 *
 * [changed] counts changed leaf values; [excluded] is the part of them that now excludes matches.
 */
internal data class FilterChange(val changed: Int, val excluded: Int = 0) {
    val isChanged: Boolean get() = changed > 0
    val included: Int get() = changed - excluded

    operator fun plus(other: FilterChange) = FilterChange(changed + other.changed, excluded + other.excluded)

    companion object {
        val None = FilterChange(0)
    }
}

/**
 * Changes of every filter in an editor tree relative to the source defaults, keyed by filter instance.
 *
 * Comparing against the defaults works for every filter type, including legacy extensions that declare no neutral
 * values, and keeps options that are selected by default from looking like user choices. Source-declared state
 * semantics remain authoritative for the leaves that provide them.
 */
internal class FilterChanges private constructor(
    private val byFilter: Map<EntryFilter<*>, FilterChange>,
    val total: FilterChange,
) {
    operator fun get(filter: EntryFilter<*>): FilterChange = byFilter[filter] ?: FilterChange.None

    companion object {
        val Empty = FilterChanges(emptyMap(), FilterChange.None)

        fun of(filters: List<EntryFilter<*>>, defaults: List<EntryFilter<*>>): FilterChanges {
            val byFilter = IdentityHashMap<EntryFilter<*>, FilterChange>()
            fun measure(filter: EntryFilter<*>, default: EntryFilter<*>?): FilterChange {
                val change = filter.changeFrom(default, ::measure)
                byFilter[filter] = change
                return change
            }
            val total = filters.withIndex().fold(FilterChange.None) { sum, (index, filter) ->
                sum + measure(filter, defaults.getOrNull(index))
            }
            return FilterChanges(byFilter, total)
        }
    }
}

private fun EntryFilter<*>.changeFrom(
    default: EntryFilter<*>?,
    measure: (EntryFilter<*>, EntryFilter<*>?) -> FilterChange,
): FilterChange {
    if (this is EntryFilter.Header || this is EntryFilter.Separator) return FilterChange.None
    if (this is EntryFilter.Group<*>) {
        val defaults = (default as? EntryFilter.Group<*>)?.state?.filterIsInstance<EntryFilter<*>>().orEmpty()
        return state.filterIsInstance<EntryFilter<*>>().withIndex().fold(FilterChange.None) { sum, (index, child) ->
            sum + measure(child, defaults.getOrNull(index))
        }
    }
    sourceStateSemantics()?.activeSelectionCount(this)?.let { return FilterChange(it) }
    if (default == null || !sameKind(default)) return FilterChange(activeCount() ?: 0)
    return when (this) {
        is EntryFilter.TriState -> when {
            state == default.state -> FilterChange.None
            isExcluded() -> FilterChange(1, excluded = 1)
            else -> FilterChange(1)
        }
        is EntryFilter.PagedGroup<*> -> {
            val defaultState = (default as EntryFilter.PagedGroup<*>).encodeCurrentState()
            if (encodeCurrentState() ==
                defaultState
            ) {
                FilterChange.None
            } else {
                FilterChange(maxOf(1, currentSelectedItemCount()))
            }
        }
        else -> if (state == default.state) FilterChange.None else FilterChange(1)
    }
}

private fun EntryFilter<*>.sameKind(other: EntryFilter<*>): Boolean = when (this) {
    is EntryFilter.TriState -> other is EntryFilter.TriState
    is EntryFilter.CheckBox -> other is EntryFilter.CheckBox
    is EntryFilter.Select<*> -> other is EntryFilter.Select<*>
    is EntryFilter.Sort -> other is EntryFilter.Sort
    is EntryFilter.Text -> other is EntryFilter.Text
    is EntryFilter.PagedGroup<*> -> other is EntryFilter.PagedGroup<*>
    is EntryFilter.Group<*> -> other is EntryFilter.Group<*>
    is EntryFilter.Header -> other is EntryFilter.Header
    is EntryFilter.Separator -> other is EntryFilter.Separator
}
