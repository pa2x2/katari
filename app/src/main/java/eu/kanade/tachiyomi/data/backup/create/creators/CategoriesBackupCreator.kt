package eu.kanade.tachiyomi.data.backup.create.creators

import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.category.model.Category
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class CategoriesBackupCreator(
    private val handler: DatabaseHandler = Injekt.get(),
    private val profileProvider: ActiveProfileProvider = Injekt.get(),
) {

    suspend operator fun invoke(): List<BackupCategory> {
        return invoke(profileProvider.activeProfileId)
    }

    suspend operator fun invoke(profileId: Long): List<BackupCategory> {
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
                )
            }
    }
}
