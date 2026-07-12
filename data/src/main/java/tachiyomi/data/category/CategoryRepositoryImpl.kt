package tachiyomi.data.category

import app.cash.sqldelight.async.coroutines.awaitAsList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.model.CategoryUpdate
import tachiyomi.domain.category.repository.CategoryRepository

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryRepositoryImpl(
    private val handler: DatabaseHandler,
    private val profileProvider: ActiveProfileProvider,
) : CategoryRepository {

    override suspend fun get(id: Long): Category? {
        return handler.awaitOneOrNull {
            categoriesQueries.getCategory(id, profileProvider.activeProfileId, ::mapCategory)
        }
    }

    override suspend fun getAll(): List<Category> {
        return handler.awaitList {
            categoriesQueries.getCategories(profileProvider.activeProfileId, ::mapCategory)
        }
    }

    override fun getAllAsFlow(): Flow<List<Category>> {
        return profileProvider.activeProfileIdFlow.flatMapLatest { profileId ->
            handler.subscribeToList { categoriesQueries.getCategories(profileId, ::mapCategory) }
        }
    }

    override suspend fun getCategoriesByEntryId(entryId: Long): List<Category> {
        return handler.awaitList {
            categoriesQueries.getCategoriesByEntryId(
                profileProvider.activeProfileId,
                entryId,
                ::mapCategory,
            )
        }
    }

    override fun getCategoriesByEntryIdAsFlow(entryId: Long): Flow<List<Category>> {
        return profileProvider.activeProfileIdFlow.flatMapLatest { profileId ->
            handler.subscribeToList {
                categoriesQueries.getCategoriesByEntryId(
                    profileId,
                    entryId,
                    ::mapCategory,
                )
            }
        }
    }

    override suspend fun getCategoryIdsByEntryIds(entryIds: List<Long>): Map<Long, List<Long>> {
        if (entryIds.isEmpty()) return emptyMap()

        return handler.await {
            categoriesQueries.getEntryCategoryMappings(profileProvider.activeProfileId, entryIds)
                .awaitAsList()
                .groupBy(
                    keySelector = { it.entry_id },
                    valueTransform = { it.category_id },
                )
        }
    }

    override suspend fun insert(category: Category) {
        handler.await {
            categoriesQueries.insert(
                profileId = profileProvider.activeProfileId,
                name = category.name,
                order = category.order,
                flags = category.flags,
            )
        }
    }

    override suspend fun updatePartial(update: CategoryUpdate) {
        handler.await {
            categoriesQueries.update(
                name = update.name,
                order = update.order,
                flags = update.flags,
                categoryId = update.id,
                profileId = profileProvider.activeProfileId,
            )
        }
    }

    override suspend fun updatePartial(updates: List<CategoryUpdate>) {
        handler.await(inTransaction = true) {
            updates.forEach { update ->
                categoriesQueries.update(
                    name = update.name,
                    order = update.order,
                    flags = update.flags,
                    categoryId = update.id,
                    profileId = profileProvider.activeProfileId,
                )
            }
        }
    }

    override suspend fun updateAllFlags(flags: Long?) {
        handler.await {
            categoriesQueries.updateAllFlags(flags, profileProvider.activeProfileId)
        }
    }

    override suspend fun delete(categoryId: Long) {
        handler.await {
            categoriesQueries.delete(
                profileId = profileProvider.activeProfileId,
                categoryId = categoryId,
            )
        }
    }

    private fun mapCategory(
        id: Long,
        name: String,
        order: Long,
        flags: Long,
    ): Category {
        return Category(
            id = id,
            name = name,
            order = order,
            flags = flags,
        )
    }
}
