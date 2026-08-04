package eu.kanade.tachiyomi.ui.updates

import cafe.adriel.voyager.core.model.ScreenModel
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.updates.service.UpdatesPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class UpdatesSettingsScreenModel(
    val updatesPreferences: UpdatesPreferences = Injekt.get(),
    val getCategories: GetCategories = Injekt.get(),
) : ScreenModel {

    val includedCategories = updatesPreferences.filterIncludedCategories
    val excludedCategories = updatesPreferences.filterExcludedCategories

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

    fun toggleFilter(preference: (UpdatesPreferences) -> Preference<TriState>) {
        preference(updatesPreferences).getAndSet {
            it.next()
        }
    }
}
