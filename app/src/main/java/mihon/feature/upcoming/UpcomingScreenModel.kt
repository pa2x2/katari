package mihon.feature.upcoming

import androidx.compose.runtime.Immutable
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapIndexedNotNull
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.core.util.insertSeparatorsReversed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.yearMonth
import mihon.domain.upcoming.interactor.GetUpcomingEntries
import mihon.feature.profiles.core.ProfileScopedStateEvent
import mihon.feature.profiles.core.observeProfileScopedState
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.source.service.HiddenSourceIds
import tachiyomi.domain.upcoming.service.UpcomingPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.time.Clock

class UpcomingScreenModel(
    private val getUpcomingEntries: GetUpcomingEntries = Injekt.get(),
    val getCategories: GetCategories = Injekt.get(),
    val upcomingPreferences: UpcomingPreferences = Injekt.get(),
    private val hiddenSourceIds: HiddenSourceIds = Injekt.get(),
    private val activeProfileProvider: ActiveProfileProvider = Injekt.get(),
) : StateScreenModel<UpcomingScreenModel.State>(State()) {

    val excludedCategories = upcomingPreferences.filterExcludedCategories
    val includedCategories = upcomingPreferences.filterIncludedCategories

    init {
        screenModelScope.launch {
            observeProfileScopedState(activeProfileProvider.activeProfileIdFlow) { profileId ->
                getUpcomingItemPreferenceFlow(profileId)
                    .distinctUntilChanged()
                    .flatMapLatest { preferences ->
                        getUpcomingEntries.subscribe(
                            profileId = profileId,
                            excludedCategories = preferences.filterExcludedCategories,
                            includedCategories = preferences.filterIncludedCategories,
                            hiddenSources = preferences.hiddenSources,
                        )
                            .distinctUntilChanged()
                            .map { preferences to it }
                    }
            }.collectLatest { event ->
                when (event) {
                    is ProfileScopedStateEvent.Reset -> mutableState.update {
                        it.copy(
                            profileId = event.profileId,
                            items = emptyList(),
                            events = emptyMap(),
                            headerIndexes = emptyMap(),
                            hasActiveFilters = false,
                            dialog = null,
                        )
                    }
                    is ProfileScopedStateEvent.Value -> mutableState.update { state ->
                        val (preferences, entries) = event.value
                        val upcomingItems = entries.toUpcomingUIModels()
                        state.copy(
                            profileId = event.profileId,
                            items = upcomingItems,
                            events = upcomingItems.toEvents(),
                            headerIndexes = upcomingItems.getHeaderIndexes(),
                            hasActiveFilters = preferences.filterIncludedCategories.isNotEmpty() ||
                                preferences.filterExcludedCategories.isNotEmpty(),
                        )
                    }
                }
            }
        }
    }

    private fun List<Entry>.toUpcomingUIModels(): List<UpcomingUIModel> {
        var entryCount = 0
        return fastMap { UpcomingUIModel.Item(it) }
            .insertSeparatorsReversed { before, after ->
                if (after != null) entryCount++

                val beforeDate = before?.entry
                    ?.expectedNextUpdate
                    ?.toLocalDateTime(TimeZone.currentSystemDefault())
                    ?.date
                val afterDate = after?.entry
                    ?.expectedNextUpdate
                    ?.toLocalDateTime(TimeZone.currentSystemDefault())
                    ?.date

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

    private fun getUpcomingItemPreferenceFlow(profileId: Long): Flow<ItemPreferences> {
        return combine(
            upcomingPreferences.filterExcludedCategories.changes(),
            upcomingPreferences.filterIncludedCategories.changes(),
            hiddenSourceIds.subscribe(profileId),
        ) { excluded, included, hiddenSources ->
            ItemPreferences(
                filterExcludedCategories = excluded,
                filterIncludedCategories = included,
                hiddenSources = hiddenSources,
            )
        }
    }

    fun resetDialog() {
        mutableState.update { it.copy(dialog = null) }
    }

    fun showFilterDialog() {
        mutableState.update { it.copy(dialog = Dialog.FilterSheet) }
    }

    fun cycleCategory(category: Category) {
        when {
            category.id in excludedCategories.get() -> {
                excludedCategories.getAndSet { it - category.id }
                includedCategories.getAndSet { it - category.id }
            }

            category.id in includedCategories.get() -> {
                includedCategories.getAndSet { it - category.id }
                excludedCategories.getAndSet { it + category.id }
            }

            else -> {
                excludedCategories.getAndSet { it - category.id }
                includedCategories.getAndSet { it + category.id }
            }
        }
    }

    @Immutable
    private data class ItemPreferences(
        val filterExcludedCategories: List<Long>,
        val filterIncludedCategories: List<Long>,
        val hiddenSources: Set<Long>,
    )

    data class State(
        val profileId: Long? = null,
        val selectedYearMonth: YearMonth = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .yearMonth,
        val items: List<UpcomingUIModel> = listOf(),
        val events: Map<LocalDate, Int> = mapOf(),
        val headerIndexes: Map<LocalDate, Int> = mapOf(),
        val hasActiveFilters: Boolean = false,
        val dialog: Dialog? = null,
    )

    sealed interface Dialog {
        data object FilterSheet : Dialog
    }
}
