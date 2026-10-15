package eu.kanade.tachiyomi.data.backup.create.creators

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupCategoryUpdateRules
import eu.kanade.tachiyomi.data.backup.models.toBackupCategoryUpdateRules
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.update.repository.LibraryUpdateRulesRepository

@Inject
class CategoriesBackupCreator(
    private val handler: DatabaseHandler,
    private val profileProvider: ActiveProfileProvider,
    private val updateRulesRepository: LibraryUpdateRulesRepository,
) {

    suspend operator fun invoke(): List<BackupCategory> {
        return invoke(profileProvider.activeProfileId)
    }

    suspend operator fun invoke(profileId: Long): List<BackupCategory> {
        val updateRules = updateRulesRepository.getCategoryRules(profileId)
        return handler.awaitList {
            categoriesQueries.getCategories(profileId) { id, name, order, flags ->
                Category(
                    id = id,
                    name = name,
                    order = order,
                    flags = flags,
                )
            }
        }
            .filterNot { it.id <= 0 }
            .map {
                BackupCategory(
                    id = it.id,
                    name = it.name,
                    order = it.order,
                    flags = it.flags,
                    updateRules = updateRules[it.id]?.toBackupCategoryUpdateRules(),
                )
            }
    }

    /** The Default category is not backed up as a category, so its update rules travel separately. */
    suspend fun defaultCategoryUpdateRules(
        profileId: Long = profileProvider.activeProfileId,
    ): BackupCategoryUpdateRules? {
        return updateRulesRepository.getCategoryRules(profileId)[Category.UNCATEGORIZED_ID]
            ?.toBackupCategoryUpdateRules()
    }
}
