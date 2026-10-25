package tachiyomi.domain.entry.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entry.model.ChapterGroupId

/** Which groups of chapters entries' chapter lists show open; groups start closed. */
interface ChapterGroupExpansionRepository {

    fun subscribeExpanded(entryIds: Collection<Long>): Flow<Set<ChapterGroupId>>

    suspend fun getExpanded(entryId: Long): Set<ChapterGroupId>

    suspend fun setExpanded(group: ChapterGroupId, expanded: Boolean)
}
