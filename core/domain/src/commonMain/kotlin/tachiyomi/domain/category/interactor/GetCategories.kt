package tachiyomi.domain.category.interactor

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.repository.CategoryRepository

@Inject
class GetCategories(
    private val categoryRepository: CategoryRepository,
) {

    fun subscribe(): Flow<List<Category>> {
        return categoryRepository.getAllAsFlow()
    }

    fun subscribeForProfile(profileId: Long): Flow<List<Category>> {
        return categoryRepository.getAllAsFlow(profileId)
    }

    fun subscribe(entryId: Long): Flow<List<Category>> {
        return categoryRepository.getCategoriesByEntryIdAsFlow(entryId)
    }

    suspend fun await(): List<Category> {
        return categoryRepository.getAll()
    }

    suspend fun awaitForProfile(profileId: Long): List<Category> {
        return categoryRepository.getAll(profileId)
    }

    suspend fun await(entryId: Long): List<Category> {
        return categoryRepository.getCategoriesByEntryId(entryId)
    }

    suspend fun await(entryId: Long, profileId: Long): List<Category> {
        return categoryRepository.getCategoriesByEntryId(entryId, profileId)
    }
}
