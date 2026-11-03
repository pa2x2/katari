package eu.kanade.tachiyomi.ui.browse.source.browse.filter

import eu.kanade.domain.source.model.applySnapshot
import eu.kanade.domain.source.model.snapshot
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.filter.EntryDateFilter
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterMetadataProvider
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterRole
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterStateSemantics
import eu.kanade.tachiyomi.source.filter.sourceStateSemantics

internal val EntryFilter<*>.metadata get() = (this as? EntryFilterMetadataProvider)?.filterMetadata
internal val EntryFilter<*>.isOrdering get() = this is EntryFilter.Sort || metadata?.role == EntryFilterRole.ORDERING

/** Null explicitly means that the source has not supplied enough semantics to count this control. */
internal fun EntryFilter<*>.activeCount(): Int? = when {
    isOrdering -> 0
    sourceStateSemantics() != null -> sourceStateSemantics()!!.activeSelectionCount(this)
    this is EntryFilter.Header || this is EntryFilter.Separator -> 0
    this is EntryDateFilter -> if (state.isBlank()) 0 else 1
    this is EntryFilter.CheckBox -> if (state) 1 else 0
    this is EntryFilter.TriState -> if (isIgnored()) 0 else 1
    this is EntryFilter.PagedGroup<*> -> currentSelectedItemCount()
    this is EntryFilter.Select<*> && metadata?.neutralOptionId != null ->
        if (metadata?.optionIds?.getOrNull(state) == metadata?.neutralOptionId) 0 else 1
    this is EntryFilter.Group<*> -> {
        val counts = state.filterIsInstance<EntryFilter<*>>().map { it.activeCount() }
        if (counts.any { it == null }) null else counts.filterNotNull().sum()
    }
    else -> null
}

internal fun EntryFilter<*>.canClear(): Boolean = when {
    isOrdering -> false
    sourceStateSemantics() != null -> sourceStateSemantics()!!.canClearSelection
    this is EntryFilter.CheckBox || this is EntryFilter.TriState -> true
    this is EntryDateFilter -> !required
    this is EntryFilter.Select<*> -> metadata?.neutralOptionId?.let { it in metadata?.optionIds.orEmpty() } == true
    this is EntryFilter.Group<*> -> state.filterIsInstance<EntryFilter<*>>()
        .filterNot { it is EntryFilter.Header || it is EntryFilter.Separator || it.isOrdering }
        .let { it.isNotEmpty() && it.all { child -> child.canClear() } }
    else -> false
}

internal fun EntryFilter<*>.clearSelection() {
    if (!canClear()) return
    sourceStateSemantics()?.let {
        it.clearSelection(this)
        return
    }
    when (this) {
        is EntryFilter.CheckBox -> state = false
        is EntryFilter.TriState -> state = EntryFilter.TriState.STATE_IGNORE
        is EntryDateFilter -> dateValue = null
        is EntryFilter.Select<*> -> state = metadata!!.optionIds.indexOf(metadata!!.neutralOptionId)
        is EntryFilter.Group<*> -> state.filterIsInstance<EntryFilter<*>>().forEach { it.clearSelection() }
        else -> Unit
    }
}

internal fun resetFilterToDefault(target: EntryFilter<*>, filters: EntryFilterList, defaults: EntryFilterList) {
    fun reset(current: List<EntryFilter<*>>, initial: List<EntryFilter<*>>): Boolean {
        current.zip(initial).forEach { (filter, default) ->
            if (filter === target) {
                EntryFilterList(filter).applySnapshot(EntryFilterList(default).snapshot())
                return true
            }
            if (filter is EntryFilter.Group<*> && default is EntryFilter.Group<*> &&
                reset(filter.state.filterIsInstance<EntryFilter<*>>(), default.state.filterIsInstance<EntryFilter<*>>())
            ) {
                return true
            }
        }
        return false
    }
    reset(filters, defaults)
}
