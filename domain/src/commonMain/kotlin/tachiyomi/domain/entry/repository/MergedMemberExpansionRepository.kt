package tachiyomi.domain.entry.repository

import kotlinx.coroutines.flow.Flow

/** Which members of merged entries show their chapters in the merged chapter list; members start collapsed. */
interface MergedMemberExpansionRepository {

    fun subscribeExpanded(entryIds: Collection<Long>): Flow<Set<Long>>

    /** Those of [entryIds] that are expanded. */
    suspend fun getExpanded(entryIds: Collection<Long>): Set<Long>

    suspend fun setExpanded(entryId: Long, expanded: Boolean)
}
