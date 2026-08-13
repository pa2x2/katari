package eu.kanade.tachiyomi.ui.browse.catalog

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.source.model.CatalogListItem
import java.util.concurrent.ConcurrentHashMap

@OptIn(FlowPreview::class)
internal class CatalogEntryStateStore(
    private val scope: CoroutineScope,
    observeEntries: suspend (List<Long>) -> Flow<List<Entry>>,
) {
    private val registeredEntryIds = ConcurrentHashMap.newKeySet<Long>()
    private val registrationGeneration = MutableStateFlow(0L)
    private val latestItems = ConcurrentHashMap<Long, CatalogListItem.EntryItem>()
    private val itemStates = ConcurrentHashMap<Long, MutableStateFlow<CatalogListItem>>()
    private val entriesById = registrationGeneration
        .debounce(REGISTRATION_DEBOUNCE_MILLIS)
        .map { registeredEntryIds.sorted() }
        .distinctUntilChanged()
        .flatMapLatest(observeEntries)
        .map { entries -> entries.associateBy(Entry::id) }
        .stateIn(scope, SharingStarted.Eagerly, emptyMap())

    init {
        entriesById.onEach { entries ->
            itemStates.forEach { (entryId, state) ->
                val latestItem = latestItems[entryId] ?: return@forEach
                val currentItem = state.value as CatalogListItem.EntryItem
                val observedEntry = entries[entryId] ?: latestItem.entry
                if (observedEntry.version >= currentItem.entry.version) {
                    state.value = latestItem.copy(entry = observedEntry)
                }
            }
        }.launchIn(scope)
    }

    fun stateFor(item: CatalogListItem.EntryItem): StateFlow<CatalogListItem> {
        val latestItem = latestItems.compute(item.entry.id) { _, current ->
            if (current == null || item.entry.version >= current.entry.version) item else current
        } ?: item
        val state = itemStates.getOrPut(item.entry.id) { MutableStateFlow(latestItem) }
        val currentItem = state.value as CatalogListItem.EntryItem
        if (latestItem.entry.version >= currentItem.entry.version) {
            state.value = latestItem
        }
        if (registeredEntryIds.add(item.entry.id)) {
            registrationGeneration.update(Long::inc)
        }
        return state
    }

    private companion object {
        const val REGISTRATION_DEBOUNCE_MILLIS = 50L
    }
}
