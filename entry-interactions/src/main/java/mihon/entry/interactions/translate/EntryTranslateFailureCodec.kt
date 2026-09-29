package mihon.entry.interactions.translate

import tachiyomi.domain.entry.model.EntryTranslationQueueItem
import tachiyomi.domain.entry.repository.EntryTranslationQueueRepository

/** Stores why a queued chapter failed in the queue's failure column, and reads it back. */
internal object EntryTranslateFailureCodec {
    private const val DOWNLOAD_FAILED = "download_failed"
    private const val DOWNLOAD_MISSING = "download_missing"
    private const val SETUP_REQUIRED = "setup_required"
    private const val ERROR_PREFIX = "error:"

    fun encode(failure: EntryTranslateFailure): String = when (failure) {
        EntryTranslateFailure.DownloadFailed -> DOWNLOAD_FAILED
        EntryTranslateFailure.DownloadMissing -> DOWNLOAD_MISSING
        EntryTranslateFailure.SetupRequired -> SETUP_REQUIRED
        is EntryTranslateFailure.Error -> ERROR_PREFIX + failure.message.orEmpty()
    }

    fun decode(value: String?): EntryTranslateFailure = when (value) {
        DOWNLOAD_FAILED -> EntryTranslateFailure.DownloadFailed
        DOWNLOAD_MISSING -> EntryTranslateFailure.DownloadMissing
        SETUP_REQUIRED -> EntryTranslateFailure.SetupRequired
        else -> EntryTranslateFailure.Error(value?.removePrefix(ERROR_PREFIX)?.takeIf { it.isNotEmpty() })
    }
}

internal suspend fun EntryTranslationQueueRepository.fail(chapterId: Long, failure: EntryTranslateFailure) =
    setState(chapterId, EntryTranslationQueueItem.State.Failed, EntryTranslateFailureCodec.encode(failure))
