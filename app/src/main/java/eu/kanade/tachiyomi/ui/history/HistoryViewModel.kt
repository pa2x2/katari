package eu.kanade.tachiyomi.ui.history

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import eu.kanade.core.util.insertSeparators
import eu.kanade.presentation.history.HistoryUiItem
import eu.kanade.presentation.history.HistoryUiModel
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.base.viewmodel.GatedStateViewModel
import eu.kanade.tachiyomi.ui.collapseByVisibleEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import logcat.LogPriority
import mihon.app.di.AppUiScope
import mihon.entry.interactions.library.membership.EntryLibraryAddRequest
import mihon.entry.interactions.library.membership.EntryLibraryAddResult
import mihon.entry.interactions.library.membership.EntryLibraryCategorySelection
import mihon.entry.interactions.library.membership.EntryLibraryDuplicatePolicy
import mihon.entry.interactions.library.membership.EntryLibraryMembershipFeature
import mihon.entry.interactions.merge.EntryMergeNavigationFeature
import mihon.entry.interactions.merge.EntryMergeSubject
import mihon.entry.interactions.navigation.EntryContinueFeature
import mihon.entry.interactions.navigation.EntryContinueResult
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
import tachiyomi.domain.statistics.repository.StatisticsRepository

@Inject
@ViewModelKey
@ContributesIntoMap(AppUiScope::class, binding = binding<ViewModel>())
class HistoryViewModel(
    private val entryLibraryMembershipFeature: EntryLibraryMembershipFeature,
    private val entryMergeNavigationFeature: EntryMergeNavigationFeature,
    private val entryContinueFeature: EntryContinueFeature,
    private val getEntry: GetEntry,
    private val getHistory: GetHistory,
    private val removeHistory: RemoveHistory,
    private val sourceManager: SourceManager,
    private val activeProfileProvider: ActiveProfileProvider,
    statisticsRepository: StatisticsRepository,
) : GatedStateViewModel<HistoryViewModel.State>(State()) {

    private val _events: Channel<Event> = Channel(Channel.UNLIMITED)
    val events: Flow<Event> = _events.receiveAsFlow()

    private val searchQuery = MutableStateFlow<String?>(null)

    /** Time spent on each day with timed activity, counted as the Activity screen for that day counts it. */
    val dayDurations: StateFlow<Map<LocalDate, Long>> = activeProfileProvider.activeProfileIdFlow
        .flatMapLatest { profileId ->
            // Open-ended, so a day that begins while the screen is open still gets its total.
            statisticsRepository.subscribeActivityTimeline(profileId, null, LAST_LOCAL_DATE)
        }
        .map { timeline ->
            timeline.activity
                .groupBy { LocalDate.parse(it.localDate) }
                .mapValues { (_, buckets) -> buckets.sumOf { it.durationMillis } }
        }
        .catch { error ->
            logcat(LogPriority.ERROR, error)
            emit(emptyMap())
        }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    init {
        gatedScope.launch {
            observeProfileScopedState(activeProfileProvider.activeProfileIdFlow) { profileId ->
                searchQuery
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
        val timeZone = TimeZone.currentSystemDefault()

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
                    memberTitle = ownerEntry?.displayTitle?.takeIf {
                        visibleEntryId != historyItem.entryId && it != visibleTitle
                    },
                    visibleCoverData = visibleCoverData,
                ),
            )
        }
            .collapseByVisibleEntry(
                actualEntryId = { it.item.historyItem.entryId },
                visibleEntryId = { it.item.visibleEntryId },
            )
            .insertSeparators { before, after ->
                val beforeDate = before?.item?.historyItem?.readAt?.toLocalDateTime(timeZone)?.date
                val afterDate = after?.item?.historyItem?.readAt?.toLocalDateTime(timeZone)?.date
                when {
                    beforeDate != afterDate && afterDate != null -> HistoryUiModel.Header(afterDate)
                    else -> null
                }
            }
    }

    suspend fun getMostRecentItem(): HistoryUiItem? {
        return withIOContext {
            mutableState.value.list
                ?.filterIsInstance<HistoryUiModel.Item>()
                ?.firstOrNull()
                ?.item
        }
    }

    fun canContinue(type: EntryType): Boolean = entryContinueFeature.isApplicable(type)

    suspend fun continueEntry(context: Context, entry: Entry): EntryContinueResult =
        entryContinueFeature.continueEntry(context, entry)

    suspend fun getEntryById(entryId: Long): Entry? {
        return withIOContext { getEntry.await(entryId) }
    }

    fun removeFromHistory(history: HistoryItem, all: Boolean = false) {
        viewModelScope.launchIO {
            if (all) {
                removeHistory.await(history.history.entryId)
            } else {
                removeHistory.await(history.history)
            }
        }
    }

    fun removeAllHistory() {
        viewModelScope.launchIO {
            if (removeHistory.awaitAll()) {
                _events.send(Event.HistoryCleared)
            }
        }
    }

    fun updateSearchQuery(query: String?) {
        searchQuery.value = query
        mutableState.update { it.copy(searchQuery = query) }
    }

    fun setDialog(dialog: Dialog?) {
        mutableState.update { it.copy(dialog = dialog) }
    }

    fun moveEntryToCategoriesAndAddToLibrary(entry: Entry, categories: List<Long>) {
        viewModelScope.launchIO {
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
        viewModelScope.launchIO {
            val entry = getEntry.await(entryId) ?: return@launchIO
            addToLibrary(EntryLibraryAddRequest(entry))
        }
    }

    fun addFavorite(entry: Entry) {
        viewModelScope.launchIO {
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

    private companion object {
        const val LAST_LOCAL_DATE = "9999-12-31"
    }
}
