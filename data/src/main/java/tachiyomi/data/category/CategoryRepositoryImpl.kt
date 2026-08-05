package tachiyomi.data.category

import app.cash.sqldelight.async.coroutines.awaitAsList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.category.model.Category
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
            getAllAsFlow(profileId)
        }
    }

    override fun getAllAsFlow(profileId: Long): Flow<List<Category>> {
        return handler.subscribeToList { categoriesQueries.getCategories(profileId, ::mapCategory) }
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
        return getCategoryIdsByEntryIds(profileProvider.activeProfileId, entryIds)
    }

    override suspend fun getCategoryIdsByEntryIds(
        profileId: Long,
        entryIds: List<Long>,
    ): Map<Long, List<Long>> {
        if (entryIds.isEmpty()) return emptyMap()

        return handler.await {
            categoriesQueries.getEntryCategoryMappings(profileId, entryIds)
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

    override suspend fun updateName(categoryId: Long, name: String) {
        handler.await {
            categoriesQueries.updateName(
                name = name,
                categoryId = categoryId,
                profileId = profileProvider.activeProfileId,
            )
        }
    }

    override suspend fun updateFlags(categoryId: Long, flags: Long) {
        handler.await {
            categoriesQueries.updateFlags(
                flags = flags,
                categoryId = categoryId,
                profileId = profileProvider.activeProfileId,
            )
        }
    }

    override suspend fun updateAllFlags(flags: Long?) {
        handler.await {
            categoriesQueries.updateAllFlags(
                flags = flags,
                profileId = profileProvider.activeProfileId,
            )
        }
    }

    override suspend fun updateAllOrders(orderedIds: List<Long>) {
        handler.await(inTransaction = true) {
            orderedIds.forEachIndexed { index, categoryId ->
                categoriesQueries.updateOrder(
                    order = index.toLong(),
                    categoryId = categoryId,
                    profileId = profileProvider.activeProfileId,
                )
            }
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
