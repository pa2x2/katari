package tachiyomi.domain.entry.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entry.model.EntryChapter

interface EntryChapterRepository {

    suspend fun getChapterById(id: Long): EntryChapter?

    fun getChaptersByEntryId(entryId: Long): Flow<List<EntryChapter>>

    fun getChaptersByEntryIds(entryIds: List<Long>): Flow<List<EntryChapter>>

    suspend fun getChaptersByEntryIdAwait(entryId: Long, applyScanlatorFilter: Boolean = false): List<EntryChapter>

    suspend fun getRecentRead(
        offset: Int = 0,
        limit: Int = 0,
    ): List<EntryChapter>

    suspend fun getBookmarkedChaptersByEntryId(entryId: Long): List<EntryChapter>

    suspend fun insert(chapter: EntryChapter): Long

    suspend fun insertOrUpdate(chapters: List<EntryChapter>): List<EntryChapter>

    suspend fun update(chapter: EntryChapter): Boolean

    suspend fun updateAll(chapters: List<EntryChapter>): Boolean

    suspend fun delete(id: Long): Boolean

    suspend fun deleteByEntryId(entryId: Long): Boolean

    suspend fun removeChaptersWithIds(chapterIds: List<Long>)

    suspend fun getScanlatorsByEntryId(entryId: Long): List<String>

    fun getScanlatorsByEntryIdAsFlow(entryId: Long): Flow<List<String>>

    suspend fun getChapterByUrlAndEntryId(url: String, entryId: Long): EntryChapter?
}
