package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressState

/** Producer-facing port for media runtimes to report facts without owning their consequences. */
fun interface EntryMediaSessionEventSink {
    suspend fun onEvent(event: EntryMediaSessionEvent): EntryMediaSessionResult
}

/** Shared boundary that coordinates independently contributed media-session consequences. */
interface EntryMediaSessionFeature : EntryMediaSessionEventSink {
    fun isApplicable(type: EntryType): Boolean
}

sealed interface EntryMediaSessionResult {
    data object Handled : EntryMediaSessionResult

    data class Inapplicable(
        val type: EntryType,
    ) : EntryMediaSessionResult
}

sealed interface EntryMediaSessionEvent {
    val visibleEntry: Entry
    val child: EntryChapter

    /**
     * A media runtime's canonical observation of progress.
     *
     * [progress] describes the observed state; it does not persist it. Feature participants decide which consequences
     * apply. [completeEquivalentChildrenByNumber] describes media equivalence used by Manga's duplicate-completion
     * policy without making the runtime execute that policy.
     */
    data class Progressed(
        override val visibleEntry: Entry,
        override val child: EntryChapter,
        val progress: EntryProgressState,
        val fraction: Double?,
        val completeEquivalentChildrenByNumber: Boolean = false,
        val deduplicateDownloadByNumber: Boolean = false,
        val preserveLocatorExtensions: Boolean = false,
        val activity: EntryMediaSessionActivity? = null,
    ) : EntryMediaSessionEvent

    /** A completed period of active reading or playback that is independent of a position update. */
    data class ActivityRecorded(
        override val visibleEntry: Entry,
        override val child: EntryChapter,
        val activity: EntryMediaSessionActivity,
    ) : EntryMediaSessionEvent
}

data class EntryMediaSessionActivity(
    val recordedAtEpochMillis: Long,
    val durationMillis: Long,
) {
    init {
        require(recordedAtEpochMillis >= 0L) { "Media-session activity time cannot be negative" }
        require(durationMillis >= 0L) { "Media-session activity duration cannot be negative" }
    }
}
