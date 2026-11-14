package eu.kanade.tachiyomi.ui.history.activity

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.katari.entry.interactions.presentation.EntryTypePresentationFeature
import app.katari.entry.interactions.statistics.EntryStatisticsFeature
import app.katari.ui.di.AppUiScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import eu.kanade.presentation.more.stats.data.StatsType
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.stats.buildStatisticsTypes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import logcat.LogPriority
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.resubscribeOnFailure
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.history.model.activity.HistoryActivityScope
import tachiyomi.domain.history.repository.HistoryActivityRepository
import tachiyomi.domain.statistics.model.StatisticsWindowActivity
import tachiyomi.domain.statistics.repository.StatisticsRepository
import java.time.LocalDate

/** @param entryIds when set, only sessions of these entries: one title, or every member of a merged title. */
@OptIn(ExperimentalCoroutinesApi::class)
@AssistedInject
class HistoryActivityViewModel(
    @Assisted private val startLocalDate: String,
    @Assisted private val endLocalDate: String,
    @Assisted typeName: String?,
    @Assisted entryIds: List<Long>?,
    private val activeProfileProvider: ActiveProfileProvider,
    private val activityRepository: HistoryActivityRepository,
    statisticsRepository: StatisticsRepository,
    statisticsFeature: EntryStatisticsFeature,
    presentationFeature: EntryTypePresentationFeature,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppUiScope::class)
    interface Factory : ManualViewModelAssistedFactory {
        fun create(
            startLocalDate: String,
            endLocalDate: String,
            typeName: String?,
            entryIds: List<Long>?,
        ): HistoryActivityViewModel
    }

    val state: StateFlow<HistoryActivityViewModel.State>
        field = MutableStateFlow<HistoryActivityViewModel.State>(State.Loading)

    val type = typeName?.let { name -> EntryType.entries.firstOrNull { it.name == name } }
    private val scope = when {
        entryIds != null -> HistoryActivityScope.Entries(entryIds)
        type != null -> HistoryActivityScope.Type(type)
        else -> HistoryActivityScope.All
    }
    val isEntryScope = scope is HistoryActivityScope.Entries
    val types: List<StatsType> = buildStatisticsTypes(statisticsFeature, presentationFeature)
    private var activeProfileId: Long? = null

    /** Totals for the whole range, independent of how many days are loaded; null until known. */
    val summary: StateFlow<HistoryActivitySummary?> = activeProfileProvider.activeProfileIdFlow
        .flatMapLatest { profileId ->
            when (scope) {
                is HistoryActivityScope.Entries ->
                    statisticsRepository.subscribeEntriesActivity(scope.entryIds, startLocalDate, endLocalDate)
                else -> statisticsRepository.subscribeActivity(profileId, startLocalDate, endLocalDate)
                    .map { it.windowActivity(type) }
            }
        }
        .map<StatisticsWindowActivity, HistoryActivitySummary?> { window ->
            summarizeHistoryActivity(window, LocalDate.parse(startLocalDate), LocalDate.parse(endLocalDate))
        }
        .resubscribeOnFailure(this@HistoryActivityViewModel) {
            emit(null)
        }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launchIO {
            activeProfileProvider.activeProfileIdFlow.collectLatest { profileId ->
                activeProfileId = profileId
                state.update { State.Loading }
                loadPage(profileId = profileId, reset = true)
            }
        }
    }

    fun retry() {
        val profileId = activeProfileId ?: return
        viewModelScope.launchIO {
            state.update { State.Loading }
            loadPage(profileId = profileId, reset = true)
        }
    }

    fun loadMore() {
        val profileId = activeProfileId ?: return
        val current = state.value as? State.Success ?: return
        if (!current.hasMore || current.loadingMore) return
        viewModelScope.launchIO {
            state.update { value ->
                (value as? State.Success)?.copy(loadingMore = true, loadMoreFailed = false) ?: value
            }
            loadPage(profileId = profileId, reset = false)
        }
    }

    private suspend fun loadPage(profileId: Long, reset: Boolean) {
        val current = state.value as? State.Success
        val dayOffset = if (reset) 0L else current?.days?.size?.toLong() ?: 0L
        try {
            val page = activityRepository.getActivityPage(
                profileId = profileId,
                startLocalDate = startLocalDate,
                endLocalDate = endLocalDate,
                scope = scope,
                dayOffset = dayOffset,
                dayLimit = DAYS_PER_PAGE,
            )
            val days = groupHistoryActivityDays(page.sessions)
            state.update { value ->
                val existing = if (reset) emptyList() else (value as? State.Success)?.days.orEmpty()
                State.Success(
                    days = (existing + days).distinctBy(HistoryActivityDay::localDate),
                    hasMore = page.hasMore,
                    loadingMore = false,
                    loadMoreFailed = false,
                )
            }
        } catch (error: Exception) {
            logcat(LogPriority.ERROR, error)
            state.update { value ->
                if (reset) {
                    State.Failed
                } else {
                    (value as? State.Success)?.copy(loadingMore = false, loadMoreFailed = true) ?: State.Failed
                }
            }
        }
    }

    sealed interface State {
        @Immutable
        data object Loading : State

        @Immutable
        data object Failed : State

        @Immutable
        data class Success(
            val days: List<HistoryActivityDay>,
            val hasMore: Boolean,
            val loadingMore: Boolean,
            val loadMoreFailed: Boolean,
        ) : State
    }

    private companion object {
        const val DAYS_PER_PAGE = 20L
    }
}
