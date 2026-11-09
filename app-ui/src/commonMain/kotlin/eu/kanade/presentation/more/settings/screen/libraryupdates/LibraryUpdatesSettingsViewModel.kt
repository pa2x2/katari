package eu.kanade.presentation.more.settings.screen.libraryupdates

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.katari.entry.interactions.state.EntryUpdateEligibilityFeature
import app.katari.library.update.pause.LibrarySourcePauses
import app.katari.library.update.planning.LibraryUpdatePlanner
import app.katari.library.update.planning.LibraryUpdatePlanningContext
import app.katari.library.update.planning.LibraryUpdatePreview
import app.katari.library.update.planning.LibraryUpdateSettings
import app.katari.library.update.planning.LibraryUpdateSkipRule
import app.katari.sync.client.libraryupdate.OtherDeviceLibraryUpdate
import app.katari.ui.di.AppUiScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.visualName
import eu.kanade.tachiyomi.ui.library.update.report.NewerOtherDeviceUpdate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.core.common.preference.minusAssign
import tachiyomi.core.common.preference.plusAssign
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.entry.interactor.GetLibraryEntries
import tachiyomi.domain.entry.service.nextupdate.FetchInterval
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.service.LibraryUpdateSchedule
import tachiyomi.domain.library.update.model.CategoryUpdateRules
import tachiyomi.domain.library.update.model.LibraryUpdateRunSummary
import tachiyomi.domain.library.update.model.subscribeLatestSummary
import tachiyomi.domain.library.update.repository.LibraryUpdateReportRepository
import tachiyomi.domain.library.update.repository.LibraryUpdateRulesRepository
import tachiyomi.domain.source.service.SourceManager
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
@Inject
@ViewModelKey
@ContributesIntoMap(AppUiScope::class)
class LibraryUpdatesSettingsViewModel(
    private val libraryUpdateSchedule: LibraryUpdateSchedule,
    activeProfileProvider: ActiveProfileProvider,
    private val libraryPreferences: LibraryPreferences,
    private val getLibraryEntries: GetLibraryEntries,
    private val getCategories: GetCategories,
    private val rulesRepository: LibraryUpdateRulesRepository,
    reportRepository: LibraryUpdateReportRepository,
    newerOtherDeviceUpdate: NewerOtherDeviceUpdate,
    private val sourceManager: SourceManager,
    fetchInterval: FetchInterval,
    eligibility: EntryUpdateEligibilityFeature,
) : ViewModel() {

    val state: StateFlow<LibraryUpdatesSettingsViewModel.State>
        field = MutableStateFlow<LibraryUpdatesSettingsViewModel.State>(State())

    private val previewCalculator = LibraryUpdatePreview.Calculator(LibraryUpdatePlanner(eligibility), eligibility)
    private val settingsReader = LibraryUpdateSettings.Reader(libraryPreferences, rulesRepository)
    private val sourcePauses = LibrarySourcePauses(libraryPreferences)
    private val contextReader = LibraryUpdatePlanningContext.Reader(reportRepository, fetchInterval)

    init {
        viewModelScope.launch {
            reportRepository.subscribeLatestSummary().collectLatest { summary ->
                state.update { it.copy(lastRun = summary) }
            }
        }
        viewModelScope.launch {
            newerOtherDeviceUpdate.subscribe().collectLatest { update ->
                state.update { it.copy(otherDeviceRun = update) }
            }
        }
        viewModelScope.launch {
            activeProfileProvider.activeProfileIdFlow
                .flatMapLatest { profileId -> libraryUpdateSchedule.nextRunFlow(profileId) }
                .collectLatest { nextRunAt ->
                    state.update { it.copy(nextRunAt = nextRunAt) }
                }
        }
        viewModelScope.launch {
            // The library settings are read again when planning, so their changes only need to trigger it.
            combine(
                getLibraryEntries.subscribe(),
                getCategories.subscribe(),
                rulesRepository.subscribeCategoryRules(),
                settingsReader.changes(),
            ) { items, categories, categoryRules, _ -> Inputs(items, categories, categoryRules) }
                .mapLatest { inputs -> inputs.toContent() }
                .collectLatest { content ->
                    state.update {
                        it.copy(
                            preview = content.preview,
                            categories = content.categories,
                            sources = content.sources,
                            types = content.types,
                        )
                    }
                }
        }
    }

    val offersDeviceRestrictions: Boolean
        get() = libraryUpdateSchedule.offersDeviceRestrictions

    fun setCategoryChecked(categoryId: Long, checked: Boolean) {
        val current = state.value.categories.find { it.category.id == categoryId }?.rules
            ?: CategoryUpdateRules(categoryId)
        setCategoryRules(current.copy(autoUpdate = checked))
    }

    fun setCategoryRules(rules: CategoryUpdateRules) {
        viewModelScope.launch {
            rulesRepository.setCategoryRules(rules)
            // A category's own interval can make the automatic update run more or less often.
            libraryUpdateSchedule.scheduleActiveProfile()
        }
    }

    fun setSourceChecked(sourceId: Long, checked: Boolean) {
        if (checked) {
            sourcePauses.resume(listOf(sourceId))
        } else {
            sourcePauses.pause(sourceId, until = null)
        }
    }

    fun setTypeChecked(type: EntryType, checked: Boolean) {
        if (checked) {
            libraryPreferences.updateExcludedEntryTypes -= type
        } else {
            libraryPreferences.updateExcludedEntryTypes += type
        }
    }

    fun setSkipRule(rule: LibraryUpdateSkipRule, on: Boolean) {
        val rules = LibraryPreferences.skipRulesOf(libraryPreferences.updateSkipRules.get())
        libraryPreferences.updateSkipRules.set(LibraryPreferences.skipRulesValues(rule.set(rules, on)))
    }

    fun setInterval(hours: Int) {
        viewModelScope.launch { libraryUpdateSchedule.scheduleActiveProfile(hours) }
    }

    fun rescheduleAfterDeviceConditionsChange() {
        viewModelScope.launch { libraryUpdateSchedule.scheduleActiveProfile() }
    }

    private suspend fun Inputs.toContent(): Content {
        val now = Clock.System.now()
        val settings = settingsReader.read(now.toEpochMilliseconds())
        val preview = previewCalculator.preview(
            items = items,
            settings = settings,
            context = contextReader.read(now),
        )

        val entriesPerCategory = items.flatMap { it.categories }.groupingBy { it }.eachCount()
        val entriesPerSource = items.flatMap { it.sourceIds }.groupingBy { it }.eachCount()
        val entriesPerType = items.groupingBy { it.entry.type }.eachCount()
        val pauses = sourcePauses.active(now.toEpochMilliseconds())
        return Content(
            preview = preview,
            categories = categories
                .filter { !it.isSystemCategory || entriesPerCategory.containsKey(it.id) }
                .map { category ->
                    CategoryRow(
                        category = category,
                        entryCount = entriesPerCategory[category.id] ?: 0,
                        rules = categoryRules[category.id] ?: CategoryUpdateRules(category.id),
                    )
                },
            sources = entriesPerSource
                .map { (sourceId, count) ->
                    SourceRow(
                        id = sourceId,
                        name = sourceManager.getDisplayInfo(sourceId).visualName(),
                        entryCount = count,
                        checked = sourceId !in pauses,
                        pausedUntil = pauses[sourceId]?.until,
                    )
                }
                .sortedBy { it.name.lowercase() },
            types = entriesPerType
                .map { (type, count) ->
                    TypeRow(type = type, entryCount = count, checked = type !in settings.excludedEntryTypes)
                }
                .sortedBy { it.type.ordinal },
        )
    }

    private class Inputs(
        val items: List<LibraryItem>,
        val categories: List<Category>,
        val categoryRules: Map<Long, CategoryUpdateRules>,
    )

    private class Content(
        val preview: LibraryUpdatePreview,
        val categories: List<CategoryRow>,
        val sources: List<SourceRow>,
        val types: List<TypeRow>,
    )

    @Immutable
    data class State(
        val lastRun: LibraryUpdateRunSummary? = null,
        /** Another device's update of the whole library that started after this device last updated it. */
        val otherDeviceRun: OtherDeviceLibraryUpdate? = null,
        val nextRunAt: Long? = null,
        /** Null until the library has loaded. */
        val preview: LibraryUpdatePreview? = null,
        val categories: List<CategoryRow> = emptyList(),
        val sources: List<SourceRow> = emptyList(),
        val types: List<TypeRow> = emptyList(),
    ) {
        /** Only Default exists, so there is nothing to choose between. */
        val hasUserCategories: Boolean
            get() = categories.any { !it.category.isSystemCategory }
    }

    data class CategoryRow(val category: Category, val entryCount: Int, val rules: CategoryUpdateRules)

    /** @param pausedUntil when the source's pause ends, or null when it isn't paused or stays paused until resumed. */
    data class SourceRow(
        val id: Long,
        val name: String,
        val entryCount: Int,
        val checked: Boolean,
        val pausedUntil: Long?,
    )

    data class TypeRow(val type: EntryType, val entryCount: Int, val checked: Boolean)
}
