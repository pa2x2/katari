package eu.kanade.tachiyomi.ui.browse.migration.entry

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.ui.browse.migration.entry.models.MigrationEntrySelectionAvailability
import eu.kanade.tachiyomi.ui.browse.migration.entry.models.MigrationEntrySelectionGroup
import eu.kanade.tachiyomi.ui.browse.migration.entry.models.MigrationEntrySelectionMember
import eu.kanade.tachiyomi.ui.browse.migration.entry.models.MigrationEntrySelectionProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import logcat.LogPriority
import mihon.app.di.AppUiScope
import mihon.core.common.utils.mutate
import mihon.entry.interactions.catalogue.EntryCatalogueFeature
import mihon.entry.interactions.merge.EntryMergeLibraryGroup
import mihon.entry.interactions.merge.EntryMergeLibraryGroupingFeature
import mihon.entry.interactions.migration.EntryMigrationAvailability
import mihon.entry.interactions.migration.EntryMigrationFeature
import mihon.entry.interactions.migration.EntryMigrationSelectionResult
import mihon.entry.interactions.migration.EntryMigrationSubject
import mihon.feature.migration.session.model.SourceMigrationGroupSelection
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.source.service.SourceManager
import kotlin.time.Duration.Companion.seconds

@AssistedInject
class MigrateEntriesViewModel(
    @Assisted private val sourceId: Long,
    private val sourceManager: SourceManager,
    private val entryRepository: EntryRepository,
    private val entryChapterRepository: EntryChapterRepository,
    private val catalogue: EntryCatalogueFeature,
    private val migration: EntryMigrationFeature,
    private val mergeGrouping: EntryMergeLibraryGroupingFeature,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppUiScope::class)
    interface Factory : ManualViewModelAssistedFactory {
        fun create(sourceId: Long): MigrateEntriesViewModel
    }

    private val _events: Channel<MigrationEntriesEvent> = Channel()
    val events: Flow<MigrationEntriesEvent> = _events.receiveAsFlow()

    private val source by lazy { sourceManager.getOrStub(sourceId) }

    private val itemOrientation by lazy { catalogue.description(source.id).itemOrientation }

    private val searchQuery = MutableStateFlow<String?>(null)

    private val selection = MutableStateFlow(emptySet<Long>())

    private val groups = entryRepository.getLibraryEntriesAsFlow()
        .flatMapLatest { entries ->
            if (entries.isEmpty()) {
                flowOf(emptyList())
            } else {
                mergeGrouping.observeLibraryGroups(entries.first().profileId, flowOf(entries))
                    .map { projection ->
                        projection.groups
                            .mapNotNull(::selectionGroup)
                            .sortedWith(
                                compareBy(String.CASE_INSENSITIVE_ORDER) {
                                    it.visibleEntry.displayTitle
                                },
                            )
                    }
            }
        }
        .flatMapLatest(::withProgress)
        .catch {
            logcat(LogPriority.ERROR, it)
            _events.send(MigrationEntriesEvent.FailedFetchingFavorites)
            emit(emptyList())
        }
        .onEach { groups ->
            val availableEntryIds = groups
                .flatMap(MigrationEntrySelectionGroup::eligibleMembers)
                .mapTo(mutableSetOf()) { it.entry.id }
            selection.update { it.intersect(availableEntryIds) }
        }

    val state: StateFlow<State> = combine(
        groups,
        searchQuery,
        selection,
    ) { groups, searchQuery, selection ->
        State(
            source = source,
            itemOrientation = itemOrientation,
            searchQuery = searchQuery,
            selection = selection,
            groupList = groups,
        )
    }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), State())

    private fun selectionGroup(group: EntryMergeLibraryGroup): MigrationEntrySelectionGroup? {
        val members = group.orderedEntries.map { entry ->
            MigrationEntrySelectionMember(
                entry = entry,
                sourceName = sourceManager.getDisplayInfo(entry.source).name,
                availability = when {
                    entry.source != sourceId -> MigrationEntrySelectionAvailability.OTHER_SOURCE
                    migration.availability(entry) is EntryMigrationAvailability.Available -> {
                        MigrationEntrySelectionAvailability.ELIGIBLE
                    }
                    else -> MigrationEntrySelectionAvailability.UNAVAILABLE
                },
            )
        }
        if (members.none { it.availability == MigrationEntrySelectionAvailability.ELIGIBLE }) return null
        return MigrationEntrySelectionGroup(group.visibleEntry, members)
    }

    private fun withProgress(
        groups: List<MigrationEntrySelectionGroup>,
    ): Flow<List<MigrationEntrySelectionGroup>> {
        val entryIds = groups
            .flatMap(MigrationEntrySelectionGroup::members)
            .map { it.entry.id }
        if (entryIds.isEmpty()) return flowOf(groups)
        return entryChapterRepository.getChaptersByEntryIds(entryIds).map { chapters ->
            val progressByEntryId = chapters
                .groupBy { it.entryId }
                .mapValues { (_, entryChapters) ->
                    MigrationEntrySelectionProgress(
                        consumedCount = entryChapters.count { it.read },
                        totalCount = entryChapters.size,
                    )
                }
            groups.map { group ->
                group.copy(
                    members = group.members.map { member ->
                        member.copy(
                            progress = progressByEntryId[member.entry.id]
                                ?: MigrationEntrySelectionProgress(),
                        )
                    },
                )
            }
        }
    }

    fun setSearchQuery(query: String?) {
        searchQuery.value = query
    }

    fun toggleSelection(itemId: Long) {
        selection.update { selection ->
            selection.mutate { list ->
                if (!list.remove(itemId)) list.add(itemId)
            }
        }
    }

    fun toggleGroupSelection(itemIds: Set<Long>) {
        selection.update { selection ->
            if (selection.containsAll(itemIds)) {
                selection - itemIds
            } else {
                selection + itemIds
            }
        }
    }

    fun toggleRangeSelection(itemId: Long) {
        val visibleItemIds = state.value.visibleItems.map { it.entry.id }
        selection.update { selection ->
            selection.mutate { selectedIds ->
                val lastSelectedId = selectedIds.lastOrNull()
                if (lastSelectedId == null) {
                    selectedIds.add(itemId)
                    return@mutate
                }

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
        }
    }

    fun selectAllVisible() {
        val visibleItemIds = state.value.visibleItems.map { it.entry.id }
        selection.update { it + visibleItemIds }
    }

    fun deselectAllVisible() {
        val visibleItemIds = state.value.visibleItems.mapTo(mutableSetOf()) { it.entry.id }
        selection.update { it - visibleItemIds }
    }

    fun migrationSelection(): List<EntryMigrationSubject> {
        val state = state.value
        val entries = state.items
            .map(MigrationEntrySelectionMember::entry)
            .filter { it.id in state.selection }
        return (migration.prepareSelection(entries) as? EntryMigrationSelectionResult.Ready)?.subjects.orEmpty()
    }

    fun migrationGroups(): List<SourceMigrationGroupSelection> {
        val selectedIds = migrationSelection().mapTo(mutableSetOf(), EntryMigrationSubject::entryId)
        if (selectedIds.isEmpty()) return emptyList()
        return state.value.groups.mapNotNull { group ->
            val selectedGroupIds = group.eligibleMembers
                .map { it.entry.id }
                .filterTo(mutableSetOf(), selectedIds::contains)
            if (selectedGroupIds.isEmpty()) return@mapNotNull null
            SourceMigrationGroupSelection(
                visibleEntryId = group.visibleEntry.id,
                memberIds = group.members.map { it.entry.id },
                selectedEntryIds = selectedGroupIds,
            )
        }
    }

    @Immutable
    data class State(
        val source: UnifiedSource? = null,
        val itemOrientation: EntryItemOrientation = EntryItemOrientation.VERTICAL,
        val searchQuery: String? = null,
        val selection: Set<Long> = emptySet(),
        private val groupList: List<MigrationEntrySelectionGroup>? = null,
    ) {

        val groups: List<MigrationEntrySelectionGroup>
            get() = groupList.orEmpty()

        val items: List<MigrationEntrySelectionMember>
            get() = groups.flatMap(MigrationEntrySelectionGroup::eligibleMembers)

        val visibleGroups: List<MigrationEntrySelectionGroup>
            get() {
                val query = searchQuery?.trim().orEmpty()
                if (query.isEmpty()) return groups
                return groups.filter { group ->
                    group.eligibleMembers.any { member -> member.entry.matchesMigrationQuery(query) }
                }
            }

        val visibleItems: List<MigrationEntrySelectionMember>
            get() = visibleGroups.flatMap(MigrationEntrySelectionGroup::eligibleMembers)

        val isLoading: Boolean
            get() = source == null || groupList == null

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
}

private fun Entry.matchesMigrationQuery(query: String): Boolean {
    return displayTitle.contains(query, ignoreCase = true) ||
        title.contains(query, ignoreCase = true) ||
        author?.contains(query, ignoreCase = true) == true ||
        artist?.contains(query, ignoreCase = true) == true
}

sealed interface MigrationEntriesEvent {
    data object FailedFetchingFavorites : MigrationEntriesEvent
}
