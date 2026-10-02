package mihon.feature.library.update.legacy

import kotlinx.coroutines.flow.first
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.update.model.CategoryUpdateRules
import tachiyomi.domain.library.update.repository.LibraryUpdateRulesRepository

/**
 * Turns the old "only update these categories" and "never update these categories" lists into category update rules.
 *
 * Excluded categories are switched off. An inclusion list switches off every other category, Default included,
 * because entries only in those were never checked. An entry in both an included and a non-included category used to
 * be checked and no longer is, since a switched-off category now wins.
 */
class LegacyUpdateCategoriesConversion(
    private val categoryRepository: CategoryRepository,
    private val rulesRepository: LibraryUpdateRulesRepository,
) {

    suspend fun convert(profileId: Long, store: PreferenceStore) {
        val included = store.getStringSet(LibraryPreferences.LEGACY_UPDATE_CATEGORIES_PREF_KEY)
        val excluded = store.getStringSet(LibraryPreferences.LEGACY_UPDATE_CATEGORIES_EXCLUDE_PREF_KEY)
        if (!included.isSet() && !excluded.isSet()) return

        val includedIds = included.get().mapNotNull(String::toLongOrNull).toSet()
        val excludedIds = excluded.get().mapNotNull(String::toLongOrNull).toSet()
        val existingRules = rulesRepository.getCategoryRules(profileId)
        categoryRepository.getAllAsFlow(profileId).first()
            .map { it.id }
            .filter { it in excludedIds || (includedIds.isNotEmpty() && it !in includedIds) }
            .forEach { categoryId ->
                val rules = existingRules[categoryId] ?: CategoryUpdateRules(categoryId)
                rulesRepository.setCategoryRules(profileId, rules.copy(autoUpdate = false))
            }

        included.delete()
        excluded.delete()
    }
}
