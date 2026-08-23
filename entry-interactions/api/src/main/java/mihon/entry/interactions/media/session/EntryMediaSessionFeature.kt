package mihon.entry.interactions.media.session

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressState
import java.util.TimeZone
import java.util.UUID

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
    val sessionId: String,
    val sequence: Long,
    val startedAtEpochMillis: Long,
    val recordedAtEpochMillis: Long,
    val durationMillis: Long,
    val timeZoneId: String,
) {
    init {
        require(sessionId.isNotBlank()) { "Media-session activity ID cannot be blank" }
        require(sequence >= 0L) { "Media-session activity sequence cannot be negative" }
        require(startedAtEpochMillis >= 0L) { "Media-session activity start time cannot be negative" }
        require(recordedAtEpochMillis >= 0L) { "Media-session activity time cannot be negative" }
        require(recordedAtEpochMillis >= startedAtEpochMillis) { "Media-session activity cannot end before it starts" }
        require(durationMillis >= 0L) { "Media-session activity duration cannot be negative" }
        require(timeZoneId.isNotBlank()) { "Media-session activity time zone cannot be blank" }
    }
}

/** Stable identity and ordering for all checkpoints produced by one logical reader or player surface. */
class EntryMediaSessionActivitySession(
    val id: String = UUID.randomUUID().toString(),
    initialSequence: Long = -1L,
) {
    private var sequence = initialSequence

    init {
        require(id.isNotBlank()) { "Media-session activity ID cannot be blank" }
        require(initialSequence >= -1L) { "Media-session activity sequence cannot be less than -1" }
    }

    @Synchronized
    fun record(
        durationMillis: Long,
        recordedAtEpochMillis: Long = System.currentTimeMillis(),
        timeZoneId: String = TimeZone.getDefault().id,
    ): EntryMediaSessionActivity {
        val safeDuration = durationMillis.coerceAtLeast(0L)
        sequence += 1L
        return EntryMediaSessionActivity(
            sessionId = id,
            sequence = sequence,
            startedAtEpochMillis = (recordedAtEpochMillis - safeDuration).coerceAtLeast(0L),
            recordedAtEpochMillis = recordedAtEpochMillis,
            durationMillis = safeDuration,
            timeZoneId = timeZoneId,
        )
    }
}
