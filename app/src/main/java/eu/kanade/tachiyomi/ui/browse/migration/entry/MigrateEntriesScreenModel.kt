package eu.kanade.tachiyomi.ui.browse.migration.entry

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.core.common.utils.mutate
import mihon.entry.interactions.migration.EntryMigrationAvailability
import mihon.entry.interactions.migration.EntryMigrationFeature
import mihon.entry.interactions.migration.EntryMigrationSelectionResult
import mihon.entry.interactions.migration.EntryMigrationSubject
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.source.service.EntrySourceDescriptionResolutionPort
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MigrateEntriesScreenModel(
    private val sourceId: Long,
    private val sourceManager: SourceManager = Injekt.get(),
    private val entryRepository: EntryRepository = Injekt.get(),
    private val entryChapterRepository: EntryChapterRepository = Injekt.get(),
    private val sourceDescription: EntrySourceDescriptionResolutionPort = Injekt.get(),
    private val migration: EntryMigrationFeature = Injekt.get(),
) : StateScreenModel<MigrateEntriesScreenModel.State>(State()) {

    private val _events: Channel<MigrationEntriesEvent> = Channel()
    val events: Flow<MigrationEntriesEvent> = _events.receiveAsFlow()

    init {
        screenModelScope.launch {
            val source = sourceManager.getOrStub(sourceId)
            mutableState.update { state ->
                state.copy(
                    source = source,
                    itemOrientation = sourceDescription.describe(source).itemOrientation,
                )
            }

            entryRepository.getFavoritesBySourceId(sourceId)
                .map { entries ->
                    entries
                        .filter { migration.availability(it) is EntryMigrationAvailability.Available }
                        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.displayTitle })
                }
                .flatMapLatest { entries ->
                    if (entries.isEmpty()) {
                        flowOf(emptyList())
                    } else {
                        entryChapterRepository.getChaptersByEntryIds(entries.map(Entry::id))
                            .map { chapters ->
                                val progressByEntryId = chapters
                                    .groupBy { it.entryId }
                                    .mapValues { (_, entryChapters) ->
                                        EntryProgress(
                                            consumedCount = entryChapters.count { it.read },
                                            totalCount = entryChapters.size,
                                        )
                                    }
                                entries.map { entry ->
                                    SelectionItem(
                                        entry = entry,
                                        progress = progressByEntryId[entry.id] ?: EntryProgress(),
                                    )
                                }
                            }
                    }
                }
                .catch {
                    logcat(LogPriority.ERROR, it)
                    _events.send(MigrationEntriesEvent.FailedFetchingFavorites)
                    mutableState.update { state ->
                        state.copy(itemList = emptyList())
                    }
                }
                .collectLatest { list ->
                    mutableState.update { state ->
                        val orderedEntryIds = list.map { it.entry.id }
                        state.copy(
                            itemList = list,
                            selection = state.selection.intersect(orderedEntryIds.toSet()),
                        )
                    }
                }
        }
    }

    fun setSearchQuery(query: String?) {
        mutableState.update { state ->
            state.copy(searchQuery = query)
        }
    }

    fun toggleSelection(itemId: Long) {
        mutableState.update { state ->
            val selection = state.selection.mutate { list ->
                if (!list.remove(itemId)) list.add(itemId)
            }
            state.copy(selection = selection)
        }
    }

    fun toggleRangeSelection(itemId: Long) {
        mutableState.update { state ->
            val selection = state.selection.mutate { selectedIds ->
                val lastSelectedId = selectedIds.lastOrNull()
                if (lastSelectedId == null) {
                    selectedIds.add(itemId)
                    return@mutate
                }

                val visibleItemIds = state.visibleItems.map { it.entry.id }
                val lastItemIndex = visibleItemIds.indexOf(lastSelectedId)
                val currentItemIndex = visibleItemIds.indexOf(itemId)
                if (lastItemIndex == -1 || currentItemIndex == -1) {
                    selectedIds.add(itemId)
                    return@mutate
                }

                val selectionRange = when {
                    lastItemIndex < currentItemIndex -> lastItemIndex..currentItemIndex
                    currentItemIndex < lastItemIndex -> currentItemIndex..lastItemIndex
                    else -> return@mutate
                }
                selectionRange.map(visibleItemIds::get).let(selectedIds::addAll)
            }
            state.copy(selection = selection)
        }
    }

    fun selectAllVisible() {
        mutableState.update { state ->
            state.copy(selection = state.selection + state.visibleItems.map { it.entry.id })
        }
    }

    fun deselectAllVisible() {
        mutableState.update { state ->
            state.copy(selection = state.selection - state.visibleItems.map { it.entry.id }.toSet())
        }
    }

    fun migrationSelection(): List<EntryMigrationSubject> {
        val state = state.value
        val entries = state.items.map(SelectionItem::entry).filter { it.id in state.selection }
        return (migration.prepareSelection(entries) as? EntryMigrationSelectionResult.Ready)?.subjects.orEmpty()
    }

    @Immutable
    data class State(
        val source: UnifiedSource? = null,
        val itemOrientation: EntryItemOrientation = EntryItemOrientation.VERTICAL,
        val searchQuery: String? = null,
        val selection: Set<Long> = emptySet(),
        private val itemList: List<SelectionItem>? = null,
    ) {

        val items: List<SelectionItem>
            get() = itemList.orEmpty()

        val visibleItems: List<SelectionItem>
            get() {
                val query = searchQuery?.trim().orEmpty()
                if (query.isEmpty()) return items
                return items.filter { item ->
                    with(item.entry) {
                        displayTitle.contains(query, ignoreCase = true) ||
                            title.contains(query, ignoreCase = true) ||
                            author?.contains(query, ignoreCase = true) == true ||
                            artist?.contains(query, ignoreCase = true) == true
                    }
                }
            }

        val isLoading: Boolean
            get() = source == null || itemList == null

        val isEmpty: Boolean
            get() = items.isEmpty()

        val hasNoSearchResults: Boolean
            get() = !isEmpty && visibleItems.isEmpty()

        val selectedCount: Int
            get() = selection.size

        val visibleSelectionCount: Int
            get() = visibleItems.count { it.entry.id in selection }

        val allVisibleSelected: Boolean
            get() = visibleItems.isNotEmpty() && visibleSelectionCount == visibleItems.size

        val selectionMode = selection.isNotEmpty()
    }

    @Immutable
    data class SelectionItem(
        val entry: Entry,
        val progress: EntryProgress,
    )

    @Immutable
    data class EntryProgress(
        val consumedCount: Int = 0,
        val totalCount: Int = 0,
    )
}

sealed interface MigrationEntriesEvent {
    data object FailedFetchingFavorites : MigrationEntriesEvent
}
