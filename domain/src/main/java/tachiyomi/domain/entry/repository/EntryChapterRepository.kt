package tachiyomi.domain.entry.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryChapterSummary

interface EntryChapterRepository {

    suspend fun getChapterById(id: Long): EntryChapter?

    fun getChaptersByEntryId(entryId: Long): Flow<List<EntryChapter>>

    fun getChaptersByEntryIds(entryIds: List<Long>): Flow<List<EntryChapter>>

    suspend fun getChaptersByEntryIdAwait(entryId: Long, applyScanlatorFilter: Boolean = false): List<EntryChapter>

    /** Every chapter of [entryIds], excluded scanlators included, in each entry's [getChaptersByEntryIdAwait] order. */
    suspend fun getChaptersByEntryIdsAwait(entryIds: Collection<Long>): List<EntryChapter>

    suspend fun getChapterSummaryByEntryId(entryId: Long): EntryChapterSummary

    suspend fun getRecentRead(
        offset: Int = 0,
        limit: Int = 0,
    ): List<EntryChapter>

    suspend fun getBookmarkedChaptersByEntryId(entryId: Long): List<EntryChapter>

    suspend fun insert(chapter: EntryChapter): Long

    /** Writes whole chapters, including the summary, preview, kind and group that follow the source. */
    suspend fun insertOrUpdate(chapters: List<EntryChapter>): List<EntryChapter>

    /** Leaves the stored summary, preview, kind and group alone; only a source sync or [insertOrUpdate] changes them. */
    suspend fun update(chapter: EntryChapter): Boolean

    suspend fun updateAll(chapters: List<EntryChapter>): Boolean

    suspend fun delete(id: Long): Boolean

    suspend fun deleteByEntryId(entryId: Long): Boolean

    /**
     * Writes a source sync's chapter changes in one transaction, so a failed sync leaves the chapters as they were.
     * [afterUpdates] runs inside that transaction once [updated] is written, and [afterInserts] once [inserted] is,
     * with their ids; repository writes made from either join the transaction.
     *
     * @return [inserted] with their database ids.
     */
    suspend fun applySourceSync(
        removedIds: List<Long>,
        updated: List<EntryChapter>,
        inserted: List<EntryChapter>,
        afterUpdates: suspend () -> Unit = {},
        afterInserts: suspend (List<EntryChapter>) -> Unit = {},
    ): List<EntryChapter>

    suspend fun getScanlatorsByEntryId(entryId: Long): List<String>

    fun getScanlatorsByEntryIdAsFlow(entryId: Long): Flow<List<String>>

    suspend fun getChapterByUrlAndEntryId(url: String, entryId: Long): EntryChapter?
}
