package mihon.feature.upcoming

import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapIndexedNotNull
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.core.util.insertSeparatorsReversed
import eu.kanade.tachiyomi.util.lang.toLocalDate
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mihon.domain.upcoming.interactor.GetUpcomingEntries
import tachiyomi.domain.entry.model.Entry
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.LocalDate
import java.time.YearMonth

class UpcomingScreenModel(
    private val getUpcomingEntries: GetUpcomingEntries = Injekt.get(),
) : StateScreenModel<UpcomingScreenModel.State>(State()) {

    init {
        screenModelScope.launch {
            getUpcomingEntries.subscribe().collectLatest {
                mutableState.update { state ->
                    val upcomingItems = it.toUpcomingUIModels()
                    state.copy(
                        items = upcomingItems,
                        events = upcomingItems.toEvents(),
                        headerIndexes = upcomingItems.getHeaderIndexes(),
                    )
                }
            }
        }
    }

    private fun List<Entry>.toUpcomingUIModels(): List<UpcomingUIModel> {
        var entryCount = 0
        return fastMap { UpcomingUIModel.Item(it) }
            .insertSeparatorsReversed { before, after ->
                if (after != null) entryCount++

                val beforeDate = before?.entry?.expectedNextUpdate?.toLocalDate()
                val afterDate = after?.entry?.expectedNextUpdate?.toLocalDate()

                if (beforeDate != afterDate && afterDate != null) {
                    UpcomingUIModel.Header(afterDate, entryCount).also { entryCount = 0 }
                } else {
                    null
                }
            }
    }

    private fun List<UpcomingUIModel>.toEvents(): Map<LocalDate, Int> {
        return filterIsInstance<UpcomingUIModel.Header>()
            .associate { it.date to it.entryCount }
    }

    private fun List<UpcomingUIModel>.getHeaderIndexes(): Map<LocalDate, Int> {
        return fastMapIndexedNotNull { index, upcomingUIModel ->
            if (upcomingUIModel is UpcomingUIModel.Header) {
                upcomingUIModel.date to index
            } else {
                null
            }
        }
            .toMap()
    }

    fun setSelectedYearMonth(yearMonth: YearMonth) {
        mutableState.update { it.copy(selectedYearMonth = yearMonth) }
    }

    data class State(
        val selectedYearMonth: YearMonth = YearMonth.now(),
        val items: List<UpcomingUIModel> = listOf(),
        val events: Map<LocalDate, Int> = mapOf(),
        val headerIndexes: Map<LocalDate, Int> = mapOf(),
    )
}
