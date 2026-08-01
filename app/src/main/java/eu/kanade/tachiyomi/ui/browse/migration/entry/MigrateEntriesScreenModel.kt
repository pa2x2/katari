package eu.kanade.tachiyomi.ui.browse.migration.entry

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.ui.browse.migration.entry.models.MigrationEntrySelectionAvailability
import eu.kanade.tachiyomi.ui.browse.migration.entry.models.MigrationEntrySelectionGroup
import eu.kanade.tachiyomi.ui.browse.migration.entry.models.MigrationEntrySelectionMember
import eu.kanade.tachiyomi.ui.browse.migration.entry.models.MigrationEntrySelectionProgress
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
import mihon.entry.interactions.catalogue.EntryCatalogueFeature
import mihon.entry.interactions.merge.EntryMergeLibraryGroup
import mihon.entry.interactions.merge.EntryMergeLibraryGroupingFeature
import mihon.entry.interactions.migration.EntryMigrationAvailability
import mihon.entry.interactions.migration.EntryMigrationFeature
import mihon.entry.interactions.migration.EntryMigrationSelectionResult
import mihon.entry.interactions.migration.EntryMigrationSubject
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MigrateEntriesScreenModel(
    private val sourceId: Long,
    private val sourceManager: SourceManager = Injekt.get(),
    private val entryRepository: EntryRepository = Injekt.get(),
    private val entryChapterRepository: EntryChapterRepository = Injekt.get(),
    private val catalogue: EntryCatalogueFeature = Injekt.get(),
    private val migration: EntryMigrationFeature = Injekt.get(),
    private val mergeGrouping: EntryMergeLibraryGroupingFeature = Injekt.get(),
) : StateScreenModel<MigrateEntriesScreenModel.State>(State()) {

    private val _events: Channel<MigrationEntriesEvent> = Channel()
    val events: Flow<MigrationEntriesEvent> = _events.receiveAsFlow()

    init {
        screenModelScope.launch {
            val source = sourceManager.getOrStub(sourceId)
            mutableState.update { state ->
                state.copy(
                    source = source,
                    itemOrientation = catalogue.description(source.id).itemOrientation,
                )
            }

            entryRepository.getLibraryEntriesAsFlow()
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
                    mutableState.update { state ->
                        state.copy(groupList = emptyList())
                    }
                }
                .collectLatest { groups ->
                    mutableState.update { state ->
                        val availableEntryIds = groups
                            .flatMap(MigrationEntrySelectionGroup::eligibleMembers)
                            .mapTo(mutableSetOf()) { it.entry.id }
                        state.copy(
                            groupList = groups,
                            selection = state.selection.intersect(availableEntryIds),
                        )
                    }
                }
        }
    }

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

    fun toggleGroupSelection(itemIds: Set<Long>) {
        mutableState.update { state ->
            val selection = if (state.selection.containsAll(itemIds)) {
                state.selection - itemIds
            } else {
                state.selection + itemIds
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
        val entries = state.items
            .map(MigrationEntrySelectionMember::entry)
            .filter { it.id in state.selection }
        return (migration.prepareSelection(entries) as? EntryMigrationSelectionResult.Ready)?.subjects.orEmpty()
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
