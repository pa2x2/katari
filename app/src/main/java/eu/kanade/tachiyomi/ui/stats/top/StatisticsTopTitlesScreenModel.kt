package eu.kanade.tachiyomi.ui.stats.top

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.presentation.more.stats.data.StatsType
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.stats.buildStatisticsTypes
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import logcat.LogPriority
import mihon.entry.interactions.presentation.EntryTypePresentationFeature
import mihon.entry.interactions.statistics.EntryStatisticsFeature
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.statistics.model.StatisticsTopEntry
import tachiyomi.domain.statistics.repository.StatisticsRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Every title with timed activity in one window, ranked by time and loaded a page at a time. */
class StatisticsTopTitlesScreenModel(
    typeName: String?,
    private val startLocalDate: String?,
    private val endLocalDate: String,
    private val activeProfileProvider: ActiveProfileProvider = Injekt.get(),
    private val statisticsRepository: StatisticsRepository = Injekt.get(),
    statisticsFeature: EntryStatisticsFeature = Injekt.get(),
    presentationFeature: EntryTypePresentationFeature = Injekt.get(),
) : StateScreenModel<StatisticsTopTitlesScreenModel.State>(State()) {

    val type = typeName?.let { name -> EntryType.entries.firstOrNull { it.name == name } }
    val types: List<StatsType> = buildStatisticsTypes(statisticsFeature, presentationFeature)
    private var loadJob: Job? = null

    init {
        loadMore()
    }

    fun loadMore() {
        val current = state.value
        if (loadJob?.isActive == true || current.endReached) return
        mutableState.update { it.copy(loading = true, failed = false) }
        loadJob = screenModelScope.launchIO {
            try {
                val page = statisticsRepository.getTopEntriesPage(
                    profileId = activeProfileProvider.activeProfileIdFlow.first(),
                    type = type,
                    startLocalDate = startLocalDate,
                    endLocalDate = endLocalDate,
                    offset = current.titles.size.toLong(),
                    limit = PAGE_SIZE,
                )
                mutableState.update {
                    it.copy(titles = it.titles + page, loading = false, endReached = page.size < PAGE_SIZE)
                }
            } catch (error: Exception) {
                logcat(LogPriority.ERROR, error)
                mutableState.update { it.copy(loading = false, failed = true) }
            }
        }
    }

    @Immutable
    data class State(
        val titles: List<StatisticsTopEntry> = emptyList(),
        val loading: Boolean = false,
        val failed: Boolean = false,
        val endReached: Boolean = false,
    )

    private companion object {
        const val PAGE_SIZE = 30L
    }
}
