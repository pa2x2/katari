package eu.kanade.tachiyomi.data.backup.restore.restorers

import app.cash.sqldelight.async.coroutines.awaitAsOne
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupCategoryUpdateRules
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.update.repository.LibraryUpdateRulesRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class CategoriesRestorer(
    private val handler: DatabaseHandler = Injekt.get(),
    private val profileProvider: ActiveProfileProvider = Injekt.get(),
    private val getCategories: GetCategories = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    private val updateRulesRepository: LibraryUpdateRulesRepository = Injekt.get(),
) {

    suspend operator fun invoke(
        backupCategories: List<BackupCategory>,
        defaultCategoryUpdateRules: BackupCategoryUpdateRules?,
    ) {
        defaultCategoryUpdateRules?.let {
            updateRulesRepository.setCategoryRules(it.toCategoryUpdateRules(Category.UNCATEGORIZED_ID))
        }
        if (backupCategories.isNotEmpty()) {
            val dbCategories = getCategories.await()
            val dbCategoriesByName = dbCategories.associateBy { it.name }
            var nextOrder = dbCategories.maxOfOrNull { it.order }?.plus(1) ?: 0

            val categories = handler.await(inTransaction = true) {
                backupCategories
                    .sortedBy { it.order }
                    .map {
                        val dbCategory = dbCategoriesByName[it.name]
                        if (dbCategory != null) return@map dbCategory
                        val order = nextOrder++
                        categoriesQueries
                            .insert(profileProvider.activeProfileId, it.name, order, it.flags)
                        categoriesQueries.selectLastInsertedRowId()
                            .awaitAsOne()
                            .let { id -> it.toCategory(id).copy(order = order) }
                    }
            }

            backupCategories.forEach { backupCategory ->
                val rules = backupCategory.updateRules ?: return@forEach
                val category = categories.firstOrNull { it.name == backupCategory.name } ?: return@forEach
                updateRulesRepository.setCategoryRules(rules.toCategoryUpdateRules(category.id))
            }

            libraryPreferences.categorizedDisplaySettings.set(
                (dbCategories + categories)
                    .distinctBy { it.flags }
                    .size > 1,
            )
        }
    }
}
