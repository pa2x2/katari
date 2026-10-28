package eu.kanade.tachiyomi.ui.browse.catalog

import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterPage
import eu.kanade.tachiyomi.source.entry.EntryFilterPageItem
import eu.kanade.tachiyomi.source.entry.EntryFilterPageRequest

internal class CatalogPagedFilterFixture : EntryFilter.PagedGroup<Set<String>>("Genres", emptySet()) {
    private class ProviderChoice(name: String, selected: Boolean) : EntryFilter.CheckBox(name, selected)
    override suspend fun getPage(
        request: EntryFilterPageRequest,
    ) = EntryFilterPage(listOf(EntryFilterPageItem("action", "Action")))
    override fun projectItem(item: EntryFilterPageItem, previous: EntryFilter<*>?): EntryFilter<*> =
        ProviderChoice(item.label, item.id in state)
    override fun reduceItemUpdate(item: EntryFilterPageItem, updatedFilter: EntryFilter<*>): Set<String> =
        if ((updatedFilter as ProviderChoice).state) state + item.id else state - item.id
    override fun selectedItemCount(state: Set<String>) = state.size
    override fun encodeState(state: Set<String>) = state.sorted().joinToString(",")
    override fun decodeState(value: String): Set<String> = value.split(',').filter { it.isNotEmpty() }.toSet()
}
