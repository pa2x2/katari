package eu.kanade.tachiyomi.ui.history

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.core.util.insertSeparators
import eu.kanade.presentation.history.HistoryUiItem
import eu.kanade.presentation.history.HistoryUiModel
import eu.kanade.tachiyomi.ui.collapseByVisibleEntry
import eu.kanade.tachiyomi.util.lang.toLocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.entry.interactions.EntryLibraryAddRequest
import mihon.entry.interactions.EntryLibraryAddResult
import mihon.entry.interactions.EntryLibraryCategorySelection
import mihon.entry.interactions.EntryLibraryDuplicatePolicy
import mihon.entry.interactions.EntryLibraryMembershipFeature
import mihon.entry.interactions.EntryMergeNavigationFeature
import mihon.entry.interactions.EntryMergeSubject
import mihon.feature.profiles.core.ProfileScopedStateEvent
import mihon.feature.profiles.core.observeProfileScopedState
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.core.common.preference.mapAsCheckboxState
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.entry.interactor.GetEntry
import tachiyomi.domain.entry.model.DuplicateEntryCandidate
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.asEntryCover
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.history.interactor.RemoveHistory
import tachiyomi.domain.history.model.HistoryItem
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.history.model.toHistoryItem
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class HistoryScreenModel(
    private val entryLibraryMembershipFeature: EntryLibraryMembershipFeature = Injekt.get(),
    private val entryMergeNavigationFeature: EntryMergeNavigationFeature = Injekt.get(),
    private val getEntry: GetEntry = Injekt.get(),
    private val getHistory: GetHistory = Injekt.get(),
    private val removeHistory: RemoveHistory = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val activeProfileProvider: ActiveProfileProvider = Injekt.get(),
    val snackbarHostState: SnackbarHostState = SnackbarHostState(),
) : StateScreenModel<HistoryScreenModel.State>(State()) {

    private val _events: Channel<Event> = Channel(Channel.UNLIMITED)
    val events: Flow<Event> = _events.receiveAsFlow()

    init {
        screenModelScope.launch {
            observeProfileScopedState(activeProfileProvider.activeProfileIdFlow) { profileId ->
                state.map { it.searchQuery }
                    .distinctUntilChanged()
                    .flatMapLatest { query ->
                        getHistory.subscribe(query ?: "", profileId)
                            .distinctUntilChanged()
                            .catch { error ->
                                logcat(LogPriority.ERROR, error)
                                _events.send(Event.InternalError)
                            }
                            .map { history -> history.toHistoryUiModels() }
                            .flowOn(Dispatchers.IO)
                    }
            }.collect { event ->
                when (event) {
                    is ProfileScopedStateEvent.Reset -> {
                        mutableState.update { it.copy(list = null, dialog = null) }
                    }
                    is ProfileScopedStateEvent.Value -> {
                        mutableState.update { it.copy(list = event.value) }
                    }
                }
            }
        }
    }

    private suspend fun List<HistoryWithRelations>.toHistoryUiModels(): List<HistoryUiModel> {
        val visibleTargetCache = mutableMapOf<Long, Long>()
        val entryCache = mutableMapOf<Long, Entry?>()

        return map { historyWithRelations ->
            val historyItem = historyWithRelations.toHistoryItem()
            val ownerEntry = entryCache.getOrPut(historyItem.entryId) { getEntry.await(historyItem.entryId) }
            val visibleEntryId = visibleTargetCache.getOrPut(historyItem.entryId) {
                ownerEntry?.let { entry ->
                    entryMergeNavigationFeature.resolveNavigation(
                        EntryMergeSubject(entry.profileId, historyItem.entryId),
                    ).visibleEntryId
                } ?: historyItem.entryId
            }
            val entry = entryCache.getOrPut(visibleEntryId) {
                getEntry.await(visibleEntryId)
            }

            val visibleTitle = entry?.displayTitle ?: historyItem.entryTitle
            val visibleCoverData = entry?.asEntryCover() ?: historyItem.coverData

            HistoryUiModel.Item(
                HistoryUiItem(
                    historyItem = historyItem,
                    visibleEntryId = visibleEntryId,
                    visibleTitle = visibleTitle,
                    visibleCoverData = visibleCoverData,
                ),
            )
        }
            .collapseByVisibleEntry(
                actualEntryId = { it.item.historyItem.entryId },
                visibleEntryId = { it.item.visibleEntryId },
            )
            .insertSeparators { before, after ->
                val beforeDate = before?.item?.historyItem?.readAt?.toLocalDate()
                val afterDate = after?.item?.historyItem?.readAt?.toLocalDate()
                when {
                    beforeDate != afterDate && afterDate != null -> HistoryUiModel.Header(afterDate)
                    else -> null
                }
            }
    }

    suspend fun getMostRecentItem(): HistoryUiItem? {
        return withIOContext {
            state.value.list
                ?.filterIsInstance<HistoryUiModel.Item>()
                ?.firstOrNull()
                ?.item
        }
    }

    suspend fun getEntryById(entryId: Long): Entry? {
        return withIOContext { getEntry.await(entryId) }
    }

    fun removeFromHistory(history: HistoryItem, all: Boolean = false) {
        screenModelScope.launchIO {
            if (all) {
                removeHistory.await(history.history.entryId)
            } else {
                removeHistory.await(history.history)
            }
        }
    }

    fun removeAllHistory() {
        screenModelScope.launchIO {
            if (removeHistory.awaitAll()) {
                _events.send(Event.HistoryCleared)
            }
        }
    }

    fun updateSearchQuery(query: String?) {
        mutableState.update { it.copy(searchQuery = query) }
    }

    fun setDialog(dialog: Dialog?) {
        mutableState.update { it.copy(dialog = dialog) }
    }

    fun moveEntryToCategoriesAndAddToLibrary(entry: Entry, categories: List<Long>) {
        screenModelScope.launchIO {
            addToLibrary(
                EntryLibraryAddRequest(
                    entry = entry,
                    duplicatePolicy = EntryLibraryDuplicatePolicy.ALLOW,
                    categorySelection = EntryLibraryCategorySelection.Selected(categories),
                ),
            )
        }
    }

    fun addFavorite(entryId: Long) {
        screenModelScope.launchIO {
            val entry = getEntry.await(entryId) ?: return@launchIO
            addToLibrary(EntryLibraryAddRequest(entry))
        }
    }

    fun addFavorite(entry: Entry) {
        screenModelScope.launchIO {
            addToLibrary(
                EntryLibraryAddRequest(entry, duplicatePolicy = EntryLibraryDuplicatePolicy.ALLOW),
            )
        }
    }

    private suspend fun addToLibrary(request: EntryLibraryAddRequest) {
        when (val result = entryLibraryMembershipFeature.add(request)) {
            is EntryLibraryAddResult.DuplicateCandidates -> mutableState.update {
                it.copy(dialog = Dialog.DuplicateEntry(result.entry, result.candidates))
            }
            is EntryLibraryAddResult.CategorySelectionRequired -> mutableState.update {
                it.copy(
                    dialog = Dialog.ChangeCategory(
                        entry = result.entry,
                        initialSelection = result.categories.mapAsCheckboxState {
                            it.id in result.selectedCategoryIds
                        },
                    ),
                )
            }
            is EntryLibraryAddResult.Failed -> logcat(LogPriority.ERROR, result.cause)
            is EntryLibraryAddResult.Added,
            is EntryLibraryAddResult.AlreadyInLibrary,
            -> Unit
        }
    }

    fun showMigrateDialog(target: Entry, current: Entry) {
        mutableState.update { currentState ->
            currentState.copy(dialog = Dialog.Migrate(target = target, current = current))
        }
    }

    suspend fun getVisibleEntryId(entryId: Long): Long {
        val entry = getEntry.await(entryId) ?: return entryId
        return entryMergeNavigationFeature.resolveNavigation(EntryMergeSubject(entry.profileId, entryId)).visibleEntryId
    }

    @Immutable
    data class State(
        val searchQuery: String? = null,
        val list: List<HistoryUiModel>? = null,
        val dialog: Dialog? = null,
    )

    sealed interface Dialog {
        data object DeleteAll : Dialog
        data class Delete(val history: HistoryItem) : Dialog
        data class DuplicateEntry(val entry: Entry, val duplicates: List<DuplicateEntryCandidate>) : Dialog
        data class ChangeCategory(
            val entry: Entry,
            val initialSelection: List<CheckboxState<Category>>,
        ) : Dialog
        data class Migrate(val target: Entry, val current: Entry) : Dialog
    }

    sealed interface Event {
        data object InternalError : Event
        data object HistoryCleared : Event
    }
}
