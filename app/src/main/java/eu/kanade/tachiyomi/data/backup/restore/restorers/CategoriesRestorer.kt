package eu.kanade.tachiyomi.data.backup.restore.restorers

import app.cash.sqldelight.async.coroutines.awaitAsOne
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupCategoryUpdateRules
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.update.repository.LibraryUpdateRulesRepository

@Inject
class CategoriesRestorer(
    private val handler: DatabaseHandler,
    private val profileProvider: ActiveProfileProvider,
    private val getCategories: GetCategories,
    private val libraryPreferences: LibraryPreferences,
    private val updateRulesRepository: LibraryUpdateRulesRepository,
) {

    suspend operator fun invoke(
        backupCategories: List<BackupCategory>,
        defaultCategoryUpdateRules: BackupCategoryUpdateRules?,
    ) {
        defaultCategoryUpdateRules?.let {
            updateRulesRepository.setCategoryRules(it.toCategoryUpdateRules(Category.UNCATEGORIZED_ID))
        }
        if (backupCategories.isEmpty()) return

        val dbCategories = getCategories.await()
        val dbCategoryNames = dbCategories.mapTo(HashSet()) { it.name }
        val newCategories = backupCategories
            .filter { it.name !in dbCategoryNames }
            .sortedBy { it.order }

        val profileId = profileProvider.activeProfileId
        val newCategoryIds = handler.await(inTransaction = true) {
            newCategories.associate {
                categoriesQueries.insert(profileId = profileId, name = it.name, flags = it.flags)
                it.name to categoriesQueries.selectLastInsertedRowId().awaitAsOne()
            }
        }
        val categoryIds = dbCategories.associate { it.name to it.id } + newCategoryIds

        backupCategories.forEach { backupCategory ->
            val rules = backupCategory.updateRules ?: return@forEach
            val categoryId = categoryIds[backupCategory.name] ?: return@forEach
            updateRulesRepository.setCategoryRules(rules.toCategoryUpdateRules(categoryId))
        }

        val flags = buildSet {
            dbCategories.mapTo(this) { it.flags }
            newCategories.mapTo(this) { it.flags }
        }
        libraryPreferences.categorizedDisplaySettings.set(flags.size > 1)
    }
}
