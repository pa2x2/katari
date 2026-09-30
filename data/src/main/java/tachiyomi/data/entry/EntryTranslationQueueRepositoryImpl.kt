package tachiyomi.data.entry

import app.cash.sqldelight.async.coroutines.awaitAsOne
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.entry.model.EntryTranslationQueueItem
import tachiyomi.domain.entry.repository.EntryTranslationQueueRepository

@OptIn(ExperimentalCoroutinesApi::class)
class EntryTranslationQueueRepositoryImpl(
    private val handler: DatabaseHandler,
    private val profileProvider: ActiveProfileProvider,
) : EntryTranslationQueueRepository {

    override suspend fun getAll(): List<EntryTranslationQueueItem> {
        return handler.awaitList {
            entry_translation_queueQueries.getByProfile(profileProvider.activeProfileId, ::mapItem)
        }
    }

    override fun subscribeAll(): Flow<List<EntryTranslationQueueItem>> {
        return profileProvider.activeProfileIdFlow.flatMapLatest { profileId ->
            handler.subscribeToList { entry_translation_queueQueries.getByProfile(profileId, ::mapItem) }
        }
    }

    override suspend fun enqueue(
        entryId: Long,
        chapterIds: List<Long>,
        state: EntryTranslationQueueItem.State,
        setup: String?,
        first: Boolean,
        queuedAt: Long,
        failure: String?,
    ) {
        handler.await(inTransaction = true) {
            val range = entry_translation_queueQueries.getPositionRange(profileProvider.activeProfileId)
                .awaitAsOne()
            val start = if (first) (range.first ?: 0L) - chapterIds.size else (range.last ?: -1L) + 1
            chapterIds.forEachIndexed { index, chapterId ->
                entry_translation_queueQueries.upsert(
                    chapterId = chapterId,
                    entryId = entryId,
                    position = start + index,
                    state = state.toDb(),
                    failure = failure,
                    setup = setup,
                    queuedAt = queuedAt,
                )
            }
        }
    }

    override suspend fun setState(chapterId: Long, state: EntryTranslationQueueItem.State, failure: String?) {
        handler.await { entry_translation_queueQueries.setState(state.toDb(), failure, chapterId) }
    }

    override suspend fun moveToFront(chapterIds: List<Long>) {
        handler.await(inTransaction = true) {
            val first = entry_translation_queueQueries.getPositionRange(profileProvider.activeProfileId)
                .awaitAsOne()
                .first ?: 0L
            chapterIds.forEachIndexed { index, chapterId ->
                entry_translation_queueQueries.setPosition(first - chapterIds.size + index, chapterId)
            }
        }
    }

    override suspend fun delete(chapterIds: Collection<Long>) {
        if (chapterIds.isEmpty()) return
        handler.await { entry_translation_queueQueries.delete(chapterIds) }
    }

    private fun mapItem(
        chapterId: Long,
        entryId: Long,
        position: Long,
        state: String,
        failure: String?,
        setup: String?,
        queuedAt: Long,
    ) = EntryTranslationQueueItem(
        chapterId = chapterId,
        entryId = entryId,
        position = position,
        state = when (state) {
            WAITING_FOR_DOWNLOAD -> EntryTranslationQueueItem.State.WaitingForDownload
            QUEUED -> EntryTranslationQueueItem.State.Queued
            else -> EntryTranslationQueueItem.State.Failed
        },
        failure = failure,
        setup = setup,
        queuedAt = queuedAt,
    )

    private fun EntryTranslationQueueItem.State.toDb() = when (this) {
        EntryTranslationQueueItem.State.WaitingForDownload -> WAITING_FOR_DOWNLOAD
        EntryTranslationQueueItem.State.Queued -> QUEUED
        EntryTranslationQueueItem.State.Failed -> FAILED
    }

    private companion object {
        const val WAITING_FOR_DOWNLOAD = "waiting_for_download"
        const val QUEUED = "queued"
        const val FAILED = "failed"
    }
}
