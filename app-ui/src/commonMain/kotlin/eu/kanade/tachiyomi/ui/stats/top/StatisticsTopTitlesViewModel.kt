package eu.kanade.tachiyomi.ui.stats.top

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
import eu.kanade.presentation.more.stats.data.StatsType
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.stats.buildStatisticsTypes
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import logcat.LogPriority
import mihon.app.di.AppUiScope
import mihon.entry.interactions.presentation.EntryTypePresentationFeature
import mihon.entry.interactions.statistics.EntryStatisticsFeature
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.statistics.model.StatisticsTopEntry
import tachiyomi.domain.statistics.repository.StatisticsRepository

/** Every title with timed activity in one window, ranked by time and loaded a page at a time. */
@AssistedInject
class StatisticsTopTitlesViewModel(
    @Assisted typeName: String?,
    @Assisted private val startLocalDate: String?,
    @Assisted private val endLocalDate: String,
    private val activeProfileProvider: ActiveProfileProvider,
    private val statisticsRepository: StatisticsRepository,
    statisticsFeature: EntryStatisticsFeature,
    presentationFeature: EntryTypePresentationFeature,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppUiScope::class)
    interface Factory : ManualViewModelAssistedFactory {
        fun create(typeName: String?, startLocalDate: String?, endLocalDate: String): StatisticsTopTitlesViewModel
    }

    val state: StateFlow<StatisticsTopTitlesViewModel.State>
        field = MutableStateFlow<StatisticsTopTitlesViewModel.State>(State())

    val type = typeName?.let { name -> EntryType.entries.firstOrNull { it.name == name } }
    val types: List<StatsType> = buildStatisticsTypes(statisticsFeature, presentationFeature)
    private var loadJob: Job? = null

    init {
        loadMore()
    }

    fun loadMore() {
        val current = state.value
        if (loadJob?.isActive == true || current.endReached) return
        state.update { it.copy(loading = true, failed = false) }
        loadJob = viewModelScope.launchIO {
            try {
                val page = statisticsRepository.getTopEntriesPage(
                    profileId = activeProfileProvider.activeProfileIdFlow.first(),
                    type = type,
                    startLocalDate = startLocalDate,
                    endLocalDate = endLocalDate,
                    offset = current.titles.size.toLong(),
                    limit = PAGE_SIZE,
                )
                state.update {
                    it.copy(titles = it.titles + page, loading = false, endReached = page.size < PAGE_SIZE)
                }
            } catch (error: Exception) {
                logcat(LogPriority.ERROR, error)
                state.update { it.copy(loading = false, failed = true) }
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
