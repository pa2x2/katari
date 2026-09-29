package eu.kanade.tachiyomi.ui.browse.source.browse.filter.change

import eu.kanade.tachiyomi.source.entry.EntryFilter

/** How a changed value reads in chips and summaries. */
internal enum class FilterChangeKind {
    /** The option is now required or turned on. */
    Included,

    /** The option now excludes matches or was turned off. */
    Excluded,

    /** A named value such as a choice, text, or date. */
    Value,

    /** A paged collection with [FilterChangeLabel.value] selected items. */
    SelectedCount,
    Ascending,
    Descending,
}

/**
 * One leaf value that differs from the source default.
 *
 * [path] addresses the leaf in the filter tree; [name] is the owning filter's label when the value alone would be
 * ambiguous, and `null` for options whose own name is the value.
 */
internal data class FilterChangeLabel(
    val path: List<Int>,
    val name: String?,
    val value: String,
    val kind: FilterChangeKind,
)

/** Lists every changed leaf in display order, so each can be shown and reset on its own. */
internal fun changeLabels(filters: List<EntryFilter<*>>, defaults: List<EntryFilter<*>>): List<FilterChangeLabel> {
    val changes = FilterChanges.of(filters, defaults)
    return buildList {
        fun visit(filter: EntryFilter<*>, path: List<Int>) {
            if (filter is EntryFilter.Group<*>) {
                filter.state.filterIsInstance<EntryFilter<*>>().forEachIndexed { index, child ->
                    visit(
                        child,
                        path + index,
                    )
                }
            } else if (changes[filter].isChanged) {
                filter.changeLabel(path)?.let(::add)
            }
        }
        filters.forEachIndexed { index, filter -> visit(filter, listOf(index)) }
    }
}

/** The label of a leaf's current value, or `null` for filters that carry no value of their own. */
internal fun EntryFilter<*>.changeLabel(path: List<Int>): FilterChangeLabel? = when (this) {
    is EntryFilter.Header, is EntryFilter.Separator, is EntryFilter.Group<*> -> null
    is EntryFilter.TriState -> FilterChangeLabel(
        path,
        null,
        name,
        if (isIncluded()) FilterChangeKind.Included else FilterChangeKind.Excluded,
    )
    is EntryFilter.CheckBox -> FilterChangeLabel(
        path,
        null,
        name,
        if (state) FilterChangeKind.Included else FilterChangeKind.Excluded,
    )
    is EntryFilter.Select<*> -> values.getOrNull(state)?.let {
        FilterChangeLabel(path, name, it.toString(), FilterChangeKind.Value)
    }
    is EntryFilter.Sort -> state?.let { selection ->
        values.getOrNull(selection.index)?.let {
            FilterChangeLabel(
                path,
                name,
                it,
                if (selection.ascending) FilterChangeKind.Ascending else FilterChangeKind.Descending,
            )
        }
    }
    is EntryFilter.Text -> FilterChangeLabel(path, name, state.trim(), FilterChangeKind.Value)
    is EntryFilter.PagedGroup<*> -> FilterChangeLabel(
        path,
        name,
        currentSelectedItemCount().toString(),
        FilterChangeKind.SelectedCount,
    )
}
