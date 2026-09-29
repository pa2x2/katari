package eu.kanade.tachiyomi.ui.history.activity

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.presentation.more.stats.data.StatsType
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.stats.buildStatisticsTypes
import kotlinx.coroutines.Dispatchers
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
import mihon.entry.interactions.presentation.EntryTypePresentationFeature
import mihon.entry.interactions.statistics.EntryStatisticsFeature
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.history.model.activity.HistoryActivitySessionDetail
import tachiyomi.domain.history.repository.HistoryActivityRepository
import tachiyomi.domain.statistics.repository.StatisticsRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.LocalDate

class HistoryActivityScreenModel(
    private val startLocalDate: String,
    private val endLocalDate: String,
    typeName: String?,
    private val activeProfileProvider: ActiveProfileProvider = Injekt.get(),
    private val activityRepository: HistoryActivityRepository = Injekt.get(),
    statisticsRepository: StatisticsRepository = Injekt.get(),
    statisticsFeature: EntryStatisticsFeature = Injekt.get(),
    presentationFeature: EntryTypePresentationFeature = Injekt.get(),
) : StateScreenModel<HistoryActivityScreenModel.State>(State.Loading) {

    val type = typeName?.let { name -> EntryType.entries.firstOrNull { it.name == name } }
    val types: List<StatsType> = buildStatisticsTypes(statisticsFeature, presentationFeature)
    private var activeProfileId: Long? = null

    /** Totals for the whole range, independent of how many session pages are loaded; null until known. */
    val summary: StateFlow<HistoryActivitySummary?> = activeProfileProvider.activeProfileIdFlow
        .flatMapLatest { profileId ->
            statisticsRepository.subscribeActivity(profileId, startLocalDate, endLocalDate)
        }
        .map<_, HistoryActivitySummary?> { snapshot ->
            summarizeHistoryActivity(snapshot, type, LocalDate.parse(startLocalDate), LocalDate.parse(endLocalDate))
        }
        .catch { error ->
            logcat(LogPriority.ERROR, error)
            emit(null)
        }
        .flowOn(Dispatchers.IO)
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        screenModelScope.launchIO {
            activeProfileProvider.activeProfileIdFlow.collectLatest { profileId ->
                activeProfileId = profileId
                mutableState.update { State.Loading }
                loadPage(profileId = profileId, reset = true)
            }
        }
    }

    fun retry() {
        val profileId = activeProfileId ?: return
        screenModelScope.launchIO {
            mutableState.update { State.Loading }
            loadPage(profileId = profileId, reset = true)
        }
    }

    fun loadMore() {
        val profileId = activeProfileId ?: return
        val current = state.value as? State.Success ?: return
        if (!current.hasMore || current.loadingMore) return
        screenModelScope.launchIO {
            mutableState.update { value ->
                (value as? State.Success)?.copy(loadingMore = true, loadMoreFailed = false) ?: value
            }
            loadPage(profileId = profileId, reset = false)
        }
    }

    private suspend fun loadPage(profileId: Long, reset: Boolean) {
        val current = state.value as? State.Success
        val offset = if (reset) 0L else current?.sessions?.size?.toLong() ?: 0L
        try {
            val page = activityRepository.getActivityPage(
                profileId = profileId,
                startLocalDate = startLocalDate,
                endLocalDate = endLocalDate,
                type = type,
                offset = offset,
                limit = PAGE_SIZE,
            )
            mutableState.update { value ->
                val existing = if (reset) emptyList() else (value as? State.Success)?.sessions.orEmpty()
                State.Success(
                    sessions = (existing + page.sessions).distinctBy(HistoryActivitySessionDetail::sessionId),
                    hasMore = page.hasMore,
                    loadingMore = false,
                    loadMoreFailed = false,
                )
            }
        } catch (error: Exception) {
            logcat(LogPriority.ERROR, error)
            mutableState.update { value ->
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
            val sessions: List<HistoryActivitySessionDetail>,
            val hasMore: Boolean,
            val loadingMore: Boolean,
            val loadMoreFailed: Boolean,
        ) : State
    }

    private companion object {
        const val PAGE_SIZE = 50L
    }
}
