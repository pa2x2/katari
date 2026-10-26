package tachiyomi.data.entry

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
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

    override fun subscribe(entryIds: Collection<Long>): Flow<List<ChapterListWalk>> {
        return handler.subscribeToList { chapter_list_walksQueries.getByEntryIds(entryIds, ::mapWalk) }
    }

    override suspend fun getAll(): List<ChapterListWalk> {
        return handler.awaitList { chapter_list_walksQueries.getAll(::mapWalk) }
    }

    override suspend fun setFailure(entryId: Long, failure: String?) {
        handler.await {
            chapter_list_walksQueries.setFailure(failure = failure?.take(MAX_FAILURE_LENGTH), entryId = entryId)
        }
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
            profileId = walk.profileId,
            token = walk.token,
            nextSourceOrder = walk.nextSourceOrder,
            loaded = walk.loaded?.toLong(),
            total = walk.total?.toLong(),
            failure = walk.failure?.take(MAX_FAILURE_LENGTH),
        )
    }

    private fun mapWalk(
        entryId: Long,
        profileId: Long,
        token: String,
        nextSourceOrder: Long,
        loaded: Long?,
        total: Long?,
        failure: String?,
    ): ChapterListWalk = ChapterListWalk(
        entryId = entryId,
        profileId = profileId,
        token = token,
        nextSourceOrder = nextSourceOrder,
        loaded = loaded?.toInt(),
        total = total?.toInt(),
        failure = failure,
    )
}

private const val MAX_FAILURE_LENGTH = 1_000
