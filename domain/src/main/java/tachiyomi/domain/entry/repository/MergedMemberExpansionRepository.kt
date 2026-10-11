package tachiyomi.domain.entry.repository

import kotlinx.coroutines.flow.Flow

/** Which members of merged entries show their chapters in the merged chapter list; members start collapsed. */
interface MergedMemberExpansionRepository {

    fun subscribeExpanded(entryIds: Collection<Long>): Flow<Set<Long>>

    suspend fun isExpanded(entryId: Long): Boolean

    suspend fun setExpanded(entryId: Long, expanded: Boolean)
}
