package mihon.entry.interactions.translate.download

import kotlinx.coroutines.flow.first
import mihon.entry.interactions.download.EntryDownloadInteraction
import mihon.entry.interactions.download.EntryDownloadState
import mihon.entry.interactions.download.EntryDownloadStatus
import mihon.entry.interactions.translate.EntryTranslateFailure
import mihon.entry.interactions.translate.queue.EntryTranslateFailureCodec
import mihon.entry.interactions.translate.queue.fail
import tachiyomi.domain.entry.model.EntryTranslationQueueItem
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.EntryTranslationQueueRepository

/**
 * Moves chapters that wait for their download on: they are queued for translation once downloaded, and fail when their
 * download fails or disappears. Chapters that failed that way wait again when their download is retried.
 * [onReady] is called when chapters became ready to translate.
 */
internal class EntryTranslateDownloadChaining(
    private val repository: EntryTranslationQueueRepository,
    private val download: EntryDownloadInteraction,
    private val entries: EntryRepository,
    private val chapters: EntryChapterRepository,
    private val onReady: () -> Unit,
) {
    /** Reconciles waiting chapters with the restored download queue, then follows download status changes. */
    suspend fun run() {
        // Before the download queue is restored every waiting chapter would look abandoned.
        download.isInitializing.first { !it }
        reconcile()
        download.updates().collect(::onDownloadStatus)
    }

    private suspend fun reconcile() {
        reconcileWaiting()
        // Queued work outlives the process; start the worker in case the system dropped it.
        if (repository.getAll().any { it.state == EntryTranslationQueueItem.State.Queued }) onReady()
    }

    private suspend fun reconcileWaiting() {
        val waiting = repository.getAll().filter { it.state == EntryTranslationQueueItem.State.WaitingForDownload }
        if (waiting.isEmpty()) return
        val queuedForDownload = download.queueState.first()
            .flatMap { it.items }
            .filter { it.state != EntryDownloadState.ERROR }
            .mapTo(HashSet()) { it.childId }
        waiting.forEach { item ->
            val entry = entries.getEntryById(item.entryId)
            val chapter = chapters.getChapterById(item.chapterId)
            when {
                entry != null && chapter != null && download.isDownloaded(entry, chapter, skipCache = true) ->
                    repository.setState(item.chapterId, EntryTranslationQueueItem.State.Queued)
                item.chapterId in queuedForDownload -> Unit
                else -> repository.fail(item.chapterId, EntryTranslateFailure.DownloadMissing)
            }
        }
    }

    private suspend fun onDownloadStatus(status: EntryDownloadStatus) {
        val item = repository.getAll().firstOrNull { it.chapterId == status.chapterId } ?: return
        val waiting = item.state == EntryTranslationQueueItem.State.WaitingForDownload
        when (status.state) {
            // A chapter that failed for want of its download is translated once the user gets it downloaded.
            EntryDownloadState.DOWNLOADED -> if (waiting || item.failedForDownload()) {
                repository.setState(item.chapterId, EntryTranslationQueueItem.State.Queued)
                onReady()
            }
            EntryDownloadState.ERROR -> if (waiting) {
                repository.fail(item.chapterId, EntryTranslateFailure.DownloadFailed)
            }
            EntryDownloadState.NOT_DOWNLOADED -> if (waiting) {
                repository.fail(item.chapterId, EntryTranslateFailure.DownloadMissing)
            }
            EntryDownloadState.QUEUE, EntryDownloadState.DOWNLOADING -> if (item.failedForDownload()) {
                repository.setState(item.chapterId, EntryTranslationQueueItem.State.WaitingForDownload)
            }
        }
    }

    private fun EntryTranslationQueueItem.failedForDownload(): Boolean =
        state == EntryTranslationQueueItem.State.Failed &&
            EntryTranslateFailureCodec.decode(failure).let {
                it == EntryTranslateFailure.DownloadFailed || it == EntryTranslateFailure.DownloadMissing
            }
}
