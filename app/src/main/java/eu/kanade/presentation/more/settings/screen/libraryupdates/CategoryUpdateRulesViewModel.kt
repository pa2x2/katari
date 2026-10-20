package eu.kanade.presentation.more.settings.screen.libraryupdates

import android.app.Application
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
import eu.kanade.tachiyomi.data.library.LibraryUpdateSchedule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import mihon.app.di.AppUiScope
import mihon.feature.library.update.planning.LibraryUpdateSkipRule
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.update.model.CategoryUpdateOverrides
import tachiyomi.domain.library.update.model.CategoryUpdateRules
import tachiyomi.domain.library.update.model.LibraryUpdateSkipRules
import tachiyomi.domain.library.update.repository.LibraryUpdateRulesRepository

@AssistedInject
class CategoryUpdateRulesViewModel(
    @Assisted private val categoryId: Long,
    private val application: Application,
    private val libraryPreferences: LibraryPreferences,
    private val rulesRepository: LibraryUpdateRulesRepository,
    getCategories: GetCategories,
) : ViewModel() {

    val state: StateFlow<CategoryUpdateRulesViewModel.State>
        field = MutableStateFlow<CategoryUpdateRulesViewModel.State>(State())

    init {
        viewModelScope.launch {
            combine(
                getCategories.subscribe(),
                rulesRepository.subscribeCategoryRules(),
                libraryPreferences.updateSkipRules.changes(),
                libraryPreferences.autoUpdateInterval.changes(),
            ) { categories, rules, skipRules, interval ->
                State(
                    category = categories.find { it.id == categoryId },
                    rules = rules[categoryId] ?: CategoryUpdateRules(categoryId),
                    librarySkipRules = LibraryPreferences.skipRulesOf(skipRules),
                    libraryIntervalHours = interval,
                )
            }
                .collectLatest { newState -> state.value = newState }
        }
    }

    fun setAutoUpdate(autoUpdate: Boolean) {
        save(state.value.rules.copy(autoUpdate = autoUpdate))
    }

    /** Overrides start from the library's current values, so turning them on changes nothing until one is edited. */
    fun setUseLibraryRules(use: Boolean) {
        val overrides = if (use) {
            null
        } else {
            CategoryUpdateOverrides(skipRules = state.value.librarySkipRules, intervalHours = null)
        }
        save(state.value.rules.copy(overrides = overrides))
    }

    fun setSkipRule(rule: LibraryUpdateSkipRule, on: Boolean) {
        val overrides = state.value.rules.overrides ?: return
        save(state.value.rules.copy(overrides = overrides.copy(skipRules = rule.set(overrides.skipRules, on))))
    }

    /** @param hours null to use the library interval. */
    fun setIntervalHours(hours: Int?) {
        val overrides = state.value.rules.overrides ?: return
        save(state.value.rules.copy(overrides = overrides.copy(intervalHours = hours)))
    }

    private fun save(rules: CategoryUpdateRules) {
        // Until the category has loaded, the rules are a placeholder that would overwrite Default's.
        if (state.value.category == null) return
        viewModelScope.launch {
            rulesRepository.setCategoryRules(rules)
            LibraryUpdateSchedule.scheduleActiveProfile(application)
        }
    }

    @Immutable
    data class State(
        val category: Category? = null,
        val rules: CategoryUpdateRules = CategoryUpdateRules(Category.UNCATEGORIZED_ID),
        val librarySkipRules: LibraryUpdateSkipRules = LibraryUpdateSkipRules.None,
        val libraryIntervalHours: Int = 0,
    )

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppUiScope::class)
    interface Factory : ManualViewModelAssistedFactory {
        fun create(categoryId: Long): CategoryUpdateRulesViewModel
    }
}
