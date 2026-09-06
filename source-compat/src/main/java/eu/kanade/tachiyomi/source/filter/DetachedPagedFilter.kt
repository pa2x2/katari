package eu.kanade.tachiyomi.source.filter

import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.EntryFilterNavigation
import eu.kanade.tachiyomi.source.entry.EntryFilterNavigationRequest
import eu.kanade.tachiyomi.source.entry.EntryFilterPage
import eu.kanade.tachiyomi.source.entry.EntryFilterPageItem
import eu.kanade.tachiyomi.source.entry.EntryFilterPageRequest
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterMetadata
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterMetadataProvider
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterValidator
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterValueMigration

private interface DetachedPagedGroup {
    val defaultState: Any?
}

@Suppress("UNCHECKED_CAST")
internal fun detachedPagedGroup(
    original: EntryFilter<*>,
    current: EntryFilter.PagedGroup<*>,
    owner: EntryFilterBinding,
): EntryFilter.PagedGroup<*> {
    val source = original as EntryFilter.PagedGroup<Any?>
    val initial = (current as? DetachedPagedGroup)?.defaultState ?: source.state
    return object :
        EntryFilter.PagedGroup<Any?>(
            current.name,
            initial,
            current.options,
        ),
        DetachedEntryFilter,
        DetachedPagedGroup,
        EntryFilterMetadataProvider,
        EntryFilterValueMigration by FilterCapabilities(source),
        EntryFilterValidator by FilterCapabilities(source) {
        override val defaultState = initial
        override val sourceFilter = source
        override val binding = owner
        override val filterMetadata get() = (source as? EntryFilterMetadataProvider)?.filterMetadata
            ?: EntryFilterMetadata()
        override suspend fun getPage(request: EntryFilterPageRequest): EntryFilterPage =
            EntryFilterList(this).withSourceFilterValues { source.getPage(request) }
        override suspend fun getNavigation(request: EntryFilterNavigationRequest): EntryFilterNavigation =
            EntryFilterList(this).withSourceFilterValues { source.getNavigation(request) }
        override fun projectItem(item: EntryFilterPageItem, previous: EntryFilter<*>?): EntryFilter<*> =
            withBoundFilterValues(listOf(this)) { source.projectItem(item, previous) }
        override fun reduceItemUpdate(item: EntryFilterPageItem, updatedFilter: EntryFilter<*>): Any? =
            withBoundFilterValues(listOf(this)) { source.reduceItemUpdate(item, updatedFilter) }
        override fun selectedItemCount(state: Any?): Int = source.selectedItemCount(state)
        override fun encodeState(state: Any?): String = source.encodeState(state)
        override fun decodeState(value: String): Any? = source.decodeState(value)
    }.also { it.state = current.state }
}
