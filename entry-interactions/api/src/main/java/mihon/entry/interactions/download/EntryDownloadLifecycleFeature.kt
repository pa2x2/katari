package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

/** Producer-facing port for structured events consumed by the Download Lifecycle feature. */
fun interface EntryDownloadLifecycleEventSink {
    suspend fun onEvent(event: EntryDownloadLifecycleEvent): EntryDownloadLifecycleResult
}

/** Feature-owned boundary for event-driven download cleanup and download-ahead policy. */
interface EntryDownloadLifecycleFeature : EntryDownloadLifecycleEventSink {
    fun isApplicable(type: EntryType): Boolean
}

sealed interface EntryDownloadLifecycleResult {
    data object Handled : EntryDownloadLifecycleResult

    data class Inapplicable(
        val type: EntryType,
    ) : EntryDownloadLifecycleResult
}

sealed interface EntryDownloadLifecycleEvent {
    val visibleEntry: Entry

    data class MarkedConsumed(
        override val visibleEntry: Entry,
        val children: List<EntryChapter>,
    ) : EntryDownloadLifecycleEvent

    data class Progressed(
        override val visibleEntry: Entry,
        val child: EntryChapter,
        val fraction: Double,
        val deduplicateByNumber: Boolean = false,
    ) : EntryDownloadLifecycleEvent

    data class Completed(
        override val visibleEntry: Entry,
        val child: EntryChapter,
        val deduplicateByNumber: Boolean = false,
    ) : EntryDownloadLifecycleEvent
}
