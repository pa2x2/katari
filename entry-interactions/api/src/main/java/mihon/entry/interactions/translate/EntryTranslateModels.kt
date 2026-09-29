package mihon.entry.interactions.translate

import mihon.language.api.tag.LanguageTag

/**
 * What a chapter is translated with. It is frozen when the chapter is queued, so changing settings afterwards does not
 * change queued or finished work.
 *
 * @property engine the translation engine's id.
 * @property recognition component ids of the text recognition pipeline, for types whose text is recognized in images.
 */
data class EntryTranslateSetup(
    val contentLanguage: LanguageTag,
    val targetLanguage: LanguageTag,
    val engine: String,
    val recognition: List<String> = emptyList(),
)

/** Where a chapter stands with background translation. A chapter with none of these is not translated. */
sealed interface EntryTranslateStatus {
    /** Queued to be translated once its download finishes. */
    data object WaitingForDownload : EntryTranslateStatus

    data object Queued : EntryTranslateStatus

    data class Translating(val progress: EntryTranslateProgress) : EntryTranslateStatus

    /** A stored translation exists; it may have gaps that are translated when the chapter is read. */
    data object Translated : EntryTranslateStatus

    data class Failed(val failure: EntryTranslateFailure) : EntryTranslateStatus
}

/** Why a chapter was not translated. */
sealed interface EntryTranslateFailure {
    /** The chapter was waiting for a download that failed. */
    data object DownloadFailed : EntryTranslateFailure

    /** The chapter's download was cancelled, lost or deleted before it was translated. */
    data object DownloadMissing : EntryTranslateFailure

    /** Text recognition or translation with the queued setup needs something from the user first, such as a model. */
    data object SetupRequired : EntryTranslateFailure

    /** Recognizing or translating the chapter failed; [message] explains why when known. */
    data class Error(val message: String?) : EntryTranslateFailure
}

/** [done] of [total] pages of the chapter being translated are processed. */
data class EntryTranslateProgress(
    val done: Int,
    val total: Int,
)

/** A chapter in the translation queue, in processing order. */
data class EntryTranslateQueueItem(
    val entryId: Long,
    val chapterId: Long,
    val status: EntryTranslateStatus,
)
