package tachiyomi.data.entry

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import tachiyomi.data.Database
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.entry.model.ChapterListWalk
import tachiyomi.domain.entry.repository.EntryChapterListWalkRepository

@Inject
@SingleIn(AppScope::class)
class EntryChapterListWalkRepositoryImpl(
    private val handler: DatabaseHandler,
) : EntryChapterListWalkRepository {

    override suspend fun get(entryId: Long): ChapterListWalk? {
        return handler.awaitOneOrNull { chapter_list_walksQueries.getByEntryId(entryId, ::mapWalk) }
    }

    // Clears the listed chapters explicitly rather than through the foreign key, which replacing the row cascades only
    // when foreign keys are enforced
    override suspend fun start(walk: ChapterListWalk) {
        handler.await(inTransaction = true) {
            chapter_list_walksQueries.clearListed(walk.entryId)
            upsert(walk)
        }
    }

    override suspend fun advance(walk: ChapterListWalk) {
        handler.await { upsert(walk) }
    }

    override suspend fun finish(entryId: Long) {
        handler.await(inTransaction = true) {
            chapter_list_walksQueries.clearListed(entryId)
            chapter_list_walksQueries.delete(entryId)
        }
    }

    override suspend fun markListed(entryId: Long, chapterIds: Collection<Long>) {
        if (chapterIds.isEmpty()) return
        handler.await(inTransaction = true) {
            if (chapter_list_walksQueries.getByEntryId(entryId).awaitAsOneOrNull() == null) return@await
            chapterIds.forEach { chapter_list_walksQueries.markListed(chapterId = it, entryId = entryId) }
        }
    }

    override suspend fun getListedChapterIds(entryId: Long): Set<Long> {
        return handler.awaitList { chapter_list_walksQueries.getListedChapterIds(entryId) }.toSet()
    }

    private suspend fun Database.upsert(walk: ChapterListWalk) {
        chapter_list_walksQueries.upsert(
            entryId = walk.entryId,
            token = walk.token,
            nextSourceOrder = walk.nextSourceOrder,
            loaded = walk.loaded?.toLong(),
            total = walk.total?.toLong(),
        )
    }

    private fun mapWalk(
        entryId: Long,
        token: String,
        nextSourceOrder: Long,
        loaded: Long?,
        total: Long?,
    ): ChapterListWalk = ChapterListWalk(
        entryId = entryId,
        token = token,
        nextSourceOrder = nextSourceOrder,
        loaded = loaded?.toInt(),
        total = total?.toInt(),
    )
}
