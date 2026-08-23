package eu.kanade.tachiyomi.ui.stats.earlier

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.presentation.more.stats.data.StatsType
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.stats.buildStatisticsTypes
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import logcat.LogPriority
import mihon.entry.interactions.presentation.EntryTypePresentationFeature
import mihon.entry.interactions.statistics.EntryStatisticsFeature
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.statistics.model.StatisticsEarlierActivityDetails
import tachiyomi.domain.statistics.repository.StatisticsRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class StatisticsEarlierActivityScreenModel(
    typeName: String?,
    private val activeProfileProvider: ActiveProfileProvider = Injekt.get(),
    private val statisticsRepository: StatisticsRepository = Injekt.get(),
    statisticsFeature: EntryStatisticsFeature = Injekt.get(),
    presentationFeature: EntryTypePresentationFeature = Injekt.get(),
) : StateScreenModel<StatisticsEarlierActivityScreenModel.State>(State.Loading) {

    val type = typeName?.let { name -> EntryType.entries.firstOrNull { it.name == name } }
    val types: List<StatsType> = buildStatisticsTypes(statisticsFeature, presentationFeature)

    init {
        screenModelScope.launchIO {
            activeProfileProvider.activeProfileIdFlow.collectLatest { profileId ->
                mutableState.update { State.Loading }
                try {
                    val details = statisticsRepository.getEarlierActivityDetails(
                        profileId = profileId,
                        type = type,
                        limit = TOP_TITLE_LIMIT,
                    )
                    mutableState.update { State.Success(details) }
                } catch (error: Exception) {
                    logcat(LogPriority.ERROR, error)
                    mutableState.update { State.Failed }
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
