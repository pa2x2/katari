package tachiyomi.domain.category.repository

import kotlinx.coroutines.flow.Flow

interface LibraryCategoryMappingObserver {
    fun observeCategoryIdsByEntryIds(
        profileId: Long,
        entryIds: List<Long>,
    ): Flow<Map<Long, List<Long>>>
}
