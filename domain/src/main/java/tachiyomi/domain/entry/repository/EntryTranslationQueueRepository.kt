package tachiyomi.domain.entry.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entry.model.EntryTranslationQueueItem

/** The active profile's queue of chapters to translate in the background, in processing order. */
interface EntryTranslationQueueRepository {

    suspend fun getAll(): List<EntryTranslationQueueItem>

    fun subscribeAll(): Flow<List<EntryTranslationQueueItem>>

    /**
     * Queues [chapterIds] of [entryId] in [state], after the rest of the queue or, when [first], before it. Chapters
     * already queued are queued again with the new setup and [failure].
     */
    suspend fun enqueue(
        entryId: Long,
        chapterIds: List<Long>,
        state: EntryTranslationQueueItem.State,
        setup: String?,
        first: Boolean,
        queuedAt: Long,
        failure: String? = null,
    )

    suspend fun setState(chapterId: Long, state: EntryTranslationQueueItem.State, failure: String? = null)

    /** Moves [chapterIds] ahead of the rest of the queue, keeping their order. */
    suspend fun moveToFront(chapterIds: List<Long>)

    suspend fun delete(chapterIds: Collection<Long>)
}
