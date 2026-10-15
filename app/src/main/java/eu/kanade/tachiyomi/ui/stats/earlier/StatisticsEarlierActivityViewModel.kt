package eu.kanade.tachiyomi.ui.stats.earlier

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import logcat.LogPriority
import mihon.app.di.AppUiScope
import mihon.entry.interactions.presentation.EntryTypePresentationFeature
import mihon.entry.interactions.statistics.EntryStatisticsFeature
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.statistics.model.StatisticsEarlierActivityDetails
import tachiyomi.domain.statistics.repository.StatisticsRepository

@AssistedInject
class StatisticsEarlierActivityViewModel(
    @Assisted typeName: String?,
    private val activeProfileProvider: ActiveProfileProvider,
    private val statisticsRepository: StatisticsRepository,
    statisticsFeature: EntryStatisticsFeature,
    presentationFeature: EntryTypePresentationFeature,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppUiScope::class)
    interface Factory : ManualViewModelAssistedFactory {
        fun create(typeName: String?): StatisticsEarlierActivityViewModel
    }

    val state: StateFlow<StatisticsEarlierActivityViewModel.State>
        field = MutableStateFlow<StatisticsEarlierActivityViewModel.State>(State.Loading)

    val type = typeName?.let { name -> EntryType.entries.firstOrNull { it.name == name } }
    val types: List<StatsType> = buildStatisticsTypes(statisticsFeature, presentationFeature)

    init {
        viewModelScope.launchIO {
            activeProfileProvider.activeProfileIdFlow.collectLatest { profileId ->
                state.update { State.Loading }
                try {
                    val details = statisticsRepository.getEarlierActivityDetails(
                        profileId = profileId,
                        type = type,
                        limit = TOP_TITLE_LIMIT,
                    )
                    state.update { State.Success(details) }
                } catch (error: Exception) {
                    logcat(LogPriority.ERROR, error)
                    state.update { State.Failed }
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
        data class Success(val details: StatisticsEarlierActivityDetails) : State
    }

    private companion object {
        const val TOP_TITLE_LIMIT = 20L
    }
}
