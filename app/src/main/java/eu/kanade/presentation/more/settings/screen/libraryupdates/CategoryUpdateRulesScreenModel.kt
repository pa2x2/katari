package eu.kanade.presentation.more.settings.screen.libraryupdates

import android.app.Application
import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mihon.feature.library.update.planning.LibraryUpdateSkipRule
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.update.model.CategoryUpdateOverrides
import tachiyomi.domain.library.update.model.CategoryUpdateRules
import tachiyomi.domain.library.update.model.LibraryUpdateSkipRules
import tachiyomi.domain.library.update.repository.LibraryUpdateRulesRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class CategoryUpdateRulesScreenModel(
    private val categoryId: Long,
    private val application: Application = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    private val rulesRepository: LibraryUpdateRulesRepository = Injekt.get(),
    getCategories: GetCategories = Injekt.get(),
) : StateScreenModel<CategoryUpdateRulesScreenModel.State>(State()) {

    init {
        screenModelScope.launch {
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
                .collectLatest { state -> mutableState.update { state } }
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
        screenModelScope.launch {
            rulesRepository.setCategoryRules(rules)
            LibraryUpdateJob.setupTask(application)
        }
    }

    @Immutable
    data class State(
        val category: Category? = null,
        val rules: CategoryUpdateRules = CategoryUpdateRules(Category.UNCATEGORIZED_ID),
        val librarySkipRules: LibraryUpdateSkipRules = LibraryUpdateSkipRules.None,
        val libraryIntervalHours: Int = 0,
    )
}
