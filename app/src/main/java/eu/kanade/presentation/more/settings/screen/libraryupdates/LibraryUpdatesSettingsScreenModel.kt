package eu.kanade.presentation.more.settings.screen.libraryupdates

import android.app.Application
import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.visualName
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mihon.entry.interactions.state.EntryUpdateEligibilityFeature
import mihon.feature.library.update.planning.LibraryUpdatePlanner
import mihon.feature.library.update.planning.LibraryUpdatePlanningContext
import mihon.feature.library.update.planning.LibraryUpdatePreview
import mihon.feature.library.update.planning.LibraryUpdateSettings
import mihon.feature.library.update.planning.LibraryUpdateSkipRule
import mihon.feature.library.update.report.LibraryUpdateRunSummary
import mihon.feature.library.update.report.subscribeLatestSummary
import tachiyomi.core.common.preference.minusAssign
import tachiyomi.core.common.preference.plusAssign
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.entry.interactor.GetLibraryEntries
import tachiyomi.domain.entry.service.FetchInterval
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.update.model.CategoryUpdateRules
import tachiyomi.domain.library.update.repository.LibraryUpdateReportRepository
import tachiyomi.domain.library.update.repository.LibraryUpdateRulesRepository
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryUpdatesSettingsScreenModel(
    private val application: Application = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    private val getLibraryEntries: GetLibraryEntries = Injekt.get(),
    private val getCategories: GetCategories = Injekt.get(),
    private val rulesRepository: LibraryUpdateRulesRepository = Injekt.get(),
    reportRepository: LibraryUpdateReportRepository = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    fetchInterval: FetchInterval = Injekt.get(),
    eligibility: EntryUpdateEligibilityFeature = Injekt.get(),
) : StateScreenModel<LibraryUpdatesSettingsScreenModel.State>(State()) {

    private val previewCalculator = LibraryUpdatePreview.Calculator(LibraryUpdatePlanner(eligibility), eligibility)
    private val settingsReader = LibraryUpdateSettings.Reader(libraryPreferences, rulesRepository)
    private val contextReader = LibraryUpdatePlanningContext.Reader(reportRepository, fetchInterval)

    init {
        screenModelScope.launch {
            reportRepository.subscribeLatestSummary().collectLatest { summary ->
                mutableState.update { it.copy(lastRun = summary) }
            }
        }
        screenModelScope.launch {
            LibraryUpdateJob.nextAutomaticRunFlow(application).collectLatest { nextRunAt ->
                mutableState.update { it.copy(nextRunAt = nextRunAt) }
            }
        }
        screenModelScope.launch {
            // The library settings are read again when planning, so their changes only need to trigger it.
            combine(
                getLibraryEntries.subscribe(),
                getCategories.subscribe(),
                rulesRepository.subscribeCategoryRules(),
                settingsReader.changes(),
            ) { items, categories, categoryRules, _ -> Inputs(items, categories, categoryRules) }
                .mapLatest { inputs -> inputs.toContent() }
                .collectLatest { content ->
                    mutableState.update {
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

    fun setCategoryChecked(categoryId: Long, checked: Boolean) {
        val current = state.value.categories.find { it.category.id == categoryId }?.rules
            ?: CategoryUpdateRules(categoryId)
        setCategoryRules(current.copy(autoUpdate = checked))
    }

    fun setCategoryRules(rules: CategoryUpdateRules) {
        screenModelScope.launch {
            rulesRepository.setCategoryRules(rules)
            // A category's own interval can make the automatic update run more or less often.
            LibraryUpdateJob.setupTask(application)
        }
    }

    fun setSourceChecked(sourceId: Long, checked: Boolean) {
        if (checked) {
            libraryPreferences.updateExcludedSources -= sourceId.toString()
        } else {
            libraryPreferences.updateExcludedSources += sourceId.toString()
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
        screenModelScope.launch { LibraryUpdateJob.setupTask(application, hours) }
    }

    fun rescheduleAfterDeviceConditionsChange() {
        screenModelScope.launch { LibraryUpdateJob.setupTask(application) }
    }

    private suspend fun Inputs.toContent(): Content {
        val settings = settingsReader.read()
        val preview = previewCalculator.preview(
            items = items,
            settings = settings,
            context = contextReader.read(Clock.System.now()),
        )

        val entriesPerCategory = items.flatMap { it.categories }.groupingBy { it }.eachCount()
        val entriesPerSource = items.flatMap { it.sourceIds }.groupingBy { it }.eachCount()
        val entriesPerType = items.groupingBy { it.entry.type }.eachCount()
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
                        checked = sourceId !in settings.excludedSourceIds,
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

    data class SourceRow(val id: Long, val name: String, val entryCount: Int, val checked: Boolean)

    data class TypeRow(val type: EntryType, val entryCount: Int, val checked: Boolean)
}
