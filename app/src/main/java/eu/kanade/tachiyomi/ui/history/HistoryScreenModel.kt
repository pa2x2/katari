package eu.kanade.tachiyomi.ui.history

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.core.util.insertSeparators
import eu.kanade.domain.track.interactor.AddTracks
import eu.kanade.presentation.history.HistoryUiItem
import eu.kanade.presentation.history.HistoryUiModel
import eu.kanade.tachiyomi.data.track.EntryTrackingSource
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
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.core.common.preference.mapAsCheckboxState
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.entry.interactor.GetDuplicateLibraryEntries
import tachiyomi.domain.entry.interactor.GetEntry
import tachiyomi.domain.entry.interactor.GetMergedEntry
import tachiyomi.domain.entry.interactor.SetEntryCategories
import tachiyomi.domain.entry.interactor.SetEntryFavorite
import tachiyomi.domain.entry.model.DuplicateEntryCandidate
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.asEntryCover
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.history.interactor.RemoveHistory
import tachiyomi.domain.history.model.HistoryItem
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.history.model.toHistoryItem
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class HistoryScreenModel(
    private val addTracks: AddTracks = Injekt.get(),
    private val categoryRepository: CategoryRepository = Injekt.get(),
    private val getCategories: GetCategories = Injekt.get(),
    private val getDuplicateLibraryEntries: GetDuplicateLibraryEntries = Injekt.get(),
    private val getEntry: GetEntry = Injekt.get(),
    private val getMergedEntry: GetMergedEntry = Injekt.get(),
    private val getHistory: GetHistory = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    private val removeHistory: RemoveHistory = Injekt.get(),
    private val setEntryCategories: SetEntryCategories = Injekt.get(),
    private val setEntryFavorite: SetEntryFavorite = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    val snackbarHostState: SnackbarHostState = SnackbarHostState(),
) : StateScreenModel<HistoryScreenModel.State>(State()) {

    private val _events: Channel<Event> = Channel(Channel.UNLIMITED)
    val events: Flow<Event> = _events.receiveAsFlow()

    init {
        screenModelScope.launch {
            state.map { it.searchQuery }
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    getHistory.subscribe(query ?: "")
                        .distinctUntilChanged()
                        .catch { error ->
                            logcat(LogPriority.ERROR, error)
                            _events.send(Event.InternalError)
                        }
                        .map { history -> history.toHistoryUiModels() }
                        .flowOn(Dispatchers.IO)
                }
                .collect { newList -> mutableState.update { it.copy(list = newList) } }
        }
    }

    private suspend fun List<HistoryWithRelations>.toHistoryUiModels(): List<HistoryUiModel> {
        val visibleTargetCache = mutableMapOf<Long, Long>()
        val entryCache = mutableMapOf<Long, Entry?>()

        return map { historyWithRelations ->
            val historyItem = historyWithRelations.toHistoryItem()
            val visibleEntryId = visibleTargetCache.getOrPut(historyItem.entryId) {
                getMergedEntry.awaitVisibleTargetId(historyItem.entryId)
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

    /**
     * Get user categories.
     *
     * @return List of categories, not including the default category
     */
    suspend fun getCategories(): List<Category> {
        return getCategories.await().filterNot { it.isSystemCategory }
    }

    private fun moveEntryToCategory(entryId: Long, categories: Category?) {
        val categoryIds = listOfNotNull(categories).map { it.id }
        moveEntryToCategory(entryId, categoryIds)
    }

    private fun moveEntryToCategory(entryId: Long, categoryIds: List<Long>) {
        screenModelScope.launchIO {
            setEntryCategories.await(entryId, categoryIds)
        }
    }

    fun moveEntryToCategoriesAndAddToLibrary(entry: Entry, categories: List<Long>) {
        moveEntryToCategory(entry.id, categories)
        if (entry.favorite) return

        screenModelScope.launchIO {
            setEntryFavorite.await(entry.id, true)
        }
    }

    private suspend fun getEntryCategoryIds(entry: Entry): List<Long> {
        return categoryRepository.getCategoriesByEntryId(entry.id)
            .map { it.id }
    }

    fun addFavorite(entryId: Long) {
        screenModelScope.launchIO {
            val entry = getEntry.await(entryId) ?: return@launchIO

            val duplicates = getDuplicateLibraryEntries(entry)
            if (duplicates.isNotEmpty()) {
                mutableState.update { it.copy(dialog = Dialog.DuplicateEntry(entry, duplicates)) }
                return@launchIO
            }

            addFavorite(entry)
        }
    }

    fun addFavorite(entry: Entry) {
        screenModelScope.launchIO {
            // Move to default category if applicable
            val categories = getCategories()
            val defaultCategoryId = libraryPreferences.defaultCategory.get().toLong()
            val defaultCategory = categories.find { it.id == defaultCategoryId }

            when {
                // Default category set
                defaultCategory != null -> {
                    val result = setEntryFavorite.await(entry.id, true)
                    if (!result) return@launchIO
                    moveEntryToCategory(entry.id, defaultCategory)
                }

                // Automatic 'Default' or no categories
                defaultCategoryId == 0L || categories.isEmpty() -> {
                    val result = setEntryFavorite.await(entry.id, true)
                    if (!result) return@launchIO
                    moveEntryToCategory(entry.id, null)
                }

                // Choose a category
                else -> showChangeCategoryDialog(entry)
            }

            val source = sourceManager.getOrStub(entry.source)
            addTracks.bindEnhancedTrackers(
                entry = entry,
                source = EntryTrackingSource.from(source, sourceManager.getDisplayInfo(entry.source)),
            )
        }
    }

    fun showMigrateDialog(target: Entry, current: Entry) {
        mutableState.update { currentState ->
            currentState.copy(dialog = Dialog.Migrate(target = target, current = current))
        }
    }

    fun showChangeCategoryDialog(entry: Entry) {
        screenModelScope.launch {
            val categories = getCategories()
            val selection = getEntryCategoryIds(entry)
            mutableState.update { currentState ->
                currentState.copy(
                    dialog = Dialog.ChangeCategory(
                        entry = entry,
                        initialSelection = categories.mapAsCheckboxState { it.id in selection },
                    ),
                )
            }
        }
    }

    suspend fun getVisibleEntryId(entryId: Long): Long {
        return getMergedEntry.awaitVisibleTargetId(entryId)
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
        data class OpenChapter(val chapter: EntryChapter?) : Event
        data object InternalError : Event
        data object HistoryCleared : Event
    }
}
