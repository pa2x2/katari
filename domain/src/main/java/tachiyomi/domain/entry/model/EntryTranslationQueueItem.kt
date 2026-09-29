package tachiyomi.domain.entry.model

/**
 * A chapter queued to be translated in the background. [setup] is what it will be translated with, frozen when it was
 * queued and encoded by the translation feature; a finished item leaves the queue.
 */
data class EntryTranslationQueueItem(
    val chapterId: Long,
    val entryId: Long,
    val position: Long,
    val state: State,
    val failure: String?,
    val setup: String,
    val queuedAt: Long,
) {
    enum class State {
        /** Waits until the chapter is downloaded; fails if its download does. */
        WaitingForDownload,
        Queued,
        Failed,
    }
}
