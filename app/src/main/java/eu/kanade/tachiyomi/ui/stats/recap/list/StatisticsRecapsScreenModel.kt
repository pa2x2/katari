package eu.kanade.tachiyomi.ui.stats.recap.list

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.ui.stats.recap.delivery.listedRecapYears
import eu.kanade.tachiyomi.ui.stats.recap.delivery.subscribeUnopenedYearRecap
import eu.kanade.tachiyomi.ui.stats.recap.period.StatisticsRecapPeriod
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.statistics.recap.StatisticsRecapRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.LocalDate
import java.time.YearMonth

/** Every recap the active profile can watch: years with activity, and each finished month with activity. */
class StatisticsRecapsScreenModel(
    activeProfileProvider: ActiveProfileProvider = Injekt.get(),
    recapRepository: StatisticsRecapRepository = Injekt.get(),
) : StateScreenModel<StatisticsRecapsScreenModel.State?>(null) {

    init {
        val today = LocalDate.now()
        val currentMonth = YearMonth.from(today)
        screenModelScope.launchIO {
            @OptIn(ExperimentalCoroutinesApi::class)
            activeProfileProvider.activeProfileIdFlow
                .flatMapLatest { profileId ->
                    combine(
                        recapRepository.subscribeActiveMonths(profileId),
                        subscribeUnopenedYearRecap(today),
                    ) { activeMonths, unopened ->
                        val months = activeMonths.map(YearMonth::parse)
                        State(
                            years = listedRecapYears(months.mapTo(HashSet()) { it.year }, today),
                            months = months.filter { it < currentMonth }.map(StatisticsRecapPeriod::Month),
                            unopened = unopened,
                        )
                    }
                }
                .collectLatest { state -> mutableState.update { state } }
        }
    }

    /** [unopened] is the year recap still marked as new. */
    @Immutable
    data class State(
        val years: List<StatisticsRecapPeriod.Year>,
        val months: List<StatisticsRecapPeriod.Month>,
        val unopened: StatisticsRecapPeriod.Year?,
    )
}
