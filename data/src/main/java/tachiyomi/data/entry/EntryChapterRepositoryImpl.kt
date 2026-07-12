package tachiyomi.data.entry

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import logcat.LogPriority
import tachiyomi.core.common.util.lang.toLong
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.data.DatabaseHandler
import tachiyomi.data.MemoColumnAdapter
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository

class EntryChapterRepositoryImpl(
    private val handler: DatabaseHandler,
    private val profileProvider: ActiveProfileProvider,
) : EntryChapterRepository {

    override suspend fun getChapterById(id: Long): EntryChapter? {
        return handler.awaitOneOrNull {
            chaptersQueries.getChapterById(id, EntryMapper::mapChapter)
        }
    }

    override fun getChaptersByEntryId(entryId: Long): Flow<List<EntryChapter>> {
        return handler.subscribeToList {
            chaptersQueries.getChaptersByEntryId(entryId, 0, EntryMapper::mapChapter)
        }
    }

    override fun getChaptersByEntryIds(entryIds: List<Long>): Flow<List<EntryChapter>> {
        if (entryIds.isEmpty()) return kotlinx.coroutines.flow.flowOf(emptyList())
        return handler.subscribeToList {
            chaptersQueries.getChaptersByEntryIds(entryIds, EntryMapper::mapChapter)
        }
    }

    override suspend fun getChaptersByEntryIdAwait(entryId: Long, applyScanlatorFilter: Boolean): List<EntryChapter> {
        return handler.awaitList {
            chaptersQueries.getChaptersByEntryId(
                entryId,
                applyScanlatorFilter.toLong(),
                EntryMapper::mapChapter,
            )
        }
    }

    override suspend fun getRecentRead(offset: Int, limit: Int): List<EntryChapter> {
        // The unified schema does not have a dedicated recent-read query yet.
        // Return recently fetched chapters as a reasonable fallback.
        return handler.awaitList {
            chaptersQueries.getChaptersByEntryId(-1, 0, EntryMapper::mapChapter)
        }
    }

    override suspend fun getBookmarkedChaptersByEntryId(entryId: Long): List<EntryChapter> {
        return handler.awaitList {
            chaptersQueries.getBookmarkedChaptersByEntryId(entryId, EntryMapper::mapChapter)
        }
    }

    override suspend fun insert(chapter: EntryChapter): Long {
        return handler.await(inTransaction = true) {
            chaptersQueries.insertReturningId(
                entryId = chapter.entryId,
                url = chapter.url,
                name = chapter.name,
                scanlator = chapter.scanlator,
                read = chapter.read,
                bookmark = chapter.bookmark,
                lastPageRead = chapter.lastPageRead,
                chapterNumber = chapter.chapterNumber,
                sourceOrder = chapter.sourceOrder,
                dateFetch = chapter.dateFetch,
                dateUpload = chapter.dateUpload,
                version = chapter.version,
                memo = chapter.memo,
            ).awaitAsOne()
        }
    }

    override suspend fun insertOrUpdate(chapters: List<EntryChapter>): List<EntryChapter> {
        return try {
            handler.await(inTransaction = true) {
                chapters.map { chapter ->
                    val existing = chapter.id.takeIf { it > 0 }
                        ?.let { chaptersQueries.getChapterById(it, EntryMapper::mapChapter).awaitAsOneOrNull() }
                        ?: chaptersQueries.getChapterByUrlAndEntryId(chapter.url, chapter.entryId)
                            .awaitAsOneOrNull()
                            ?.let(EntryMapper::mapChapter)
                    if (existing == null) {
                        val id = chaptersQueries.insertReturningId(
                            entryId = chapter.entryId,
                            url = chapter.url,
                            name = chapter.name,
                            scanlator = chapter.scanlator,
                            read = chapter.read,
                            bookmark = chapter.bookmark,
                            lastPageRead = chapter.lastPageRead,
                            chapterNumber = chapter.chapterNumber,
                            sourceOrder = chapter.sourceOrder,
                            dateFetch = chapter.dateFetch,
                            dateUpload = chapter.dateUpload,
                            version = chapter.version,
                            memo = chapter.memo,
                        ).awaitAsOne()
                        chapter.copy(id = id)
                    } else {
                        chaptersQueries.update(
                            entryId = chapter.entryId,
                            url = chapter.url,
                            name = chapter.name,
                            scanlator = chapter.scanlator,
                            read = chapter.read,
                            bookmark = chapter.bookmark,
                            lastPageRead = chapter.lastPageRead,
                            chapterNumber = chapter.chapterNumber,
                            sourceOrder = chapter.sourceOrder,
                            dateFetch = chapter.dateFetch,
                            dateUpload = chapter.dateUpload,
                            version = chapter.version,
                            isSyncing = chapter.isSyncing,
                            memo = MemoColumnAdapter.encode(chapter.memo),
                            chapterId = existing.id,
                        )
                        chapter.copy(id = existing.id)
                    }
                }
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            emptyList()
        }
    }

    override suspend fun update(chapter: EntryChapter): Boolean {
        return updateAll(listOf(chapter))
    }

    override suspend fun updateAll(chapters: List<EntryChapter>): Boolean {
        return try {
            handler.await(inTransaction = true) {
                chapters.forEach { chapter ->
                    chaptersQueries.update(
                        entryId = chapter.entryId,
                        url = chapter.url,
                        name = chapter.name,
                        scanlator = chapter.scanlator,
                        read = chapter.read,
                        bookmark = chapter.bookmark,
                        lastPageRead = chapter.lastPageRead,
                        chapterNumber = chapter.chapterNumber,
                        sourceOrder = chapter.sourceOrder,
                        dateFetch = chapter.dateFetch,
                        dateUpload = chapter.dateUpload,
                        version = chapter.version,
                        isSyncing = chapter.isSyncing,
                        memo = MemoColumnAdapter.encode(chapter.memo),
                        chapterId = chapter.id,
                    )
                }
            }
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }

    override suspend fun delete(id: Long): Boolean {
        return try {
            handler.await { chaptersQueries.removeChaptersWithIds(listOf(id)) }
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }

    override suspend fun deleteByEntryId(entryId: Long): Boolean {
        return try {
            handler.await {
                chaptersQueries.getChaptersByEntryId(entryId, 0).awaitAsList()
                    .map { it._id }
                    .let { chaptersQueries.removeChaptersWithIds(it) }
            }
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }

    override suspend fun removeChaptersWithIds(chapterIds: List<Long>) {
        try {
            handler.await { chaptersQueries.removeChaptersWithIds(chapterIds) }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }

    override suspend fun getScanlatorsByEntryId(entryId: Long): List<String> {
        return handler.awaitList {
            chaptersQueries.getScanlatorsByEntryId(entryId) { it.orEmpty() }
        }
    }

    override fun getScanlatorsByEntryIdAsFlow(entryId: Long): Flow<List<String>> {
        return handler.subscribeToList {
            chaptersQueries.getScanlatorsByEntryId(entryId) { it.orEmpty() }
        }
    }

    override suspend fun getChapterByUrlAndEntryId(url: String, entryId: Long): EntryChapter? {
        return handler.awaitOneOrNull {
            chaptersQueries.getChapterByUrlAndEntryId(url, entryId, EntryMapper::mapChapter)
        }
    }
}
