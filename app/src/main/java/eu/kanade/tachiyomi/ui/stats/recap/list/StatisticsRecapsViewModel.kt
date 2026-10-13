package eu.kanade.tachiyomi.ui.stats.recap.list

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.ui.stats.recap.delivery.listedRecapYears
import eu.kanade.tachiyomi.ui.stats.recap.delivery.subscribeUnopenedYearRecap
import eu.kanade.tachiyomi.ui.stats.recap.period.StatisticsRecapPeriod
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.statistics.recap.StatisticsRecapRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.LocalDate
import java.time.YearMonth

/** Every recap the active profile can watch: years with activity, and each finished month with activity. */
class StatisticsRecapsViewModel(
    activeProfileProvider: ActiveProfileProvider = Injekt.get(),
    recapRepository: StatisticsRecapRepository = Injekt.get(),
) : ViewModel() {

    val state: StateFlow<StatisticsRecapsViewModel.State?>
        field = MutableStateFlow<StatisticsRecapsViewModel.State?>(null)

    init {
        val today = LocalDate.now()
        val currentMonth = YearMonth.from(today)
        viewModelScope.launchIO {
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
                .collectLatest { newState -> state.value = newState }
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
