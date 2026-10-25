package tachiyomi.domain.entry.repository

import tachiyomi.domain.entry.model.ChapterListWalk

interface EntryChapterListWalkRepository {

    suspend fun get(entryId: Long): ChapterListWalk?

    /** Starts [walk], replacing an unfinished walk of the same entry and forgetting the chapters it listed. */
    suspend fun start(walk: ChapterListWalk)

    /** Moves [walk] on to its next part, keeping the chapters it listed. */
    suspend fun advance(walk: ChapterListWalk)

    /** Ends the entry's walk, if any. */
    suspend fun finish(entryId: Long)

    /** Records that the entry's walk listed [chapterIds]; ignored when the entry has no walk. */
    suspend fun markListed(entryId: Long, chapterIds: Collection<Long>)

    suspend fun getListedChapterIds(entryId: Long): Set<Long>
}
