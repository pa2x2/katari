package mihon.entry.interactions.translate

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

/**
 * Translates downloaded chapters in the background and keeps the results with their downloads, so a chapter opened
 * later shows them straight away.
 */
interface EntryTranslateFeature {
    fun isApplicable(type: EntryType): Boolean

    /** The active profile's queue, in processing order. */
    val queue: Flow<List<EntryTranslateQueueItem>>

    /**
     * Resolves what [entry]'s chapters would be translated with from its languages and the profile's settings, and
     * what the user must resolve first. `null` when the feature does not apply to [entry].
     */
    suspend fun prepare(entry: Entry): EntryTranslatePreparation?

    /** Statuses of [entry]'s chapters that are translated, queued or failed. */
    fun observeStatuses(entry: Entry): Flow<Map<Long, EntryTranslateStatus>>

    /**
     * Queues [chapters] to be translated with [setup]. Chapters not downloaded yet wait for their download, which must
     * already be queued; when [startNow] is set they go ahead of the rest of the queue.
     */
    suspend fun translate(entry: Entry, chapters: List<EntryChapter>, setup: EntryTranslateSetup, startNow: Boolean)

    /** Moves queued chapters ahead of the rest of the queue. */
    suspend fun startNow(chapterIds: List<Long>)

    /** Queues failed chapters again with the setup they were queued with. */
    suspend fun retry(chapterIds: List<Long>)

    /** Removes chapters from the queue; a chapter being translated stops and keeps no partial result. */
    suspend fun cancel(chapterIds: List<Long>)

    /** Deletes the stored translations of [chapters] and keeps their downloads. */
    suspend fun deleteTranslation(entry: Entry, chapters: List<EntryChapter>)
}
