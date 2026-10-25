package tachiyomi.domain.entry.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entry.model.EntryChapterGroup

interface EntryChapterGroupRepository {

    /** The groups of [entryIds], each entry's in reading order. */
    fun subscribe(entryIds: List<Long>): Flow<List<EntryChapterGroup>>

    suspend fun getByEntryId(entryId: Long): List<EntryChapterGroup>

    /** Makes [groups] the whole group list of [entryId]. */
    suspend fun replace(entryId: Long, groups: List<EntryChapterGroup>)
}
