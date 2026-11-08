package mihon.entry.interactions.migration.host

import mihon.entry.interactions.migration.EntryMigrationMode
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.track.model.EntryTrack

/** One Migration-owned optimistic database transition; it is not a reusable Entry or child mutation API. */
data class EntryMigrationHostTransition(
    val operationId: String,
    val intentFingerprint: String,
    val profileId: Long,
    val mode: EntryMigrationMode,
    val expectedSource: Entry,
    val expectedTarget: Entry,
    val expectedSourceCategoryIds: List<Long>?,
    val expectedTargetChildren: List<EntryChapter>?,
    val sourceUpdate: Entry?,
    val targetUpdate: Entry,
    val targetCategoryIds: List<Long>?,
    val childUpdates: List<EntryMigrationHostChildUpdate>,
    /**
     * For a replacement, the old entry's chapters whose history moves to the new entry's chapter that stands for each,
     * by ID; the old entry's sessions and completions move with them, so the new entry keeps the time read. Null for a
     * copy, which leaves it with the old entry, still in the Library.
     */
    val historyChapterMoves: Map<Long, Long>?,
    val expectedSourceTracks: List<EntryTrack>,
    val preparedTracks: List<EntryTrack>,
    val consequenceRequests: List<EntryMigrationConsequenceRequest> = emptyList(),
)

data class EntryMigrationHostChildUpdate(
    val expected: EntryChapter,
    val updated: EntryChapter,
)

data class EntryMigrationConsequenceRequest(
    val participantId: String,
    val schemaVersion: Int,
    val payload: String,
) {
    init {
        require(participantId.isNotBlank()) { "Migration consequence participant ID cannot be blank" }
        require(schemaVersion > 0) { "Migration consequence schema version must be positive" }
    }
}

sealed interface EntryMigrationHostTransitionResult {
    data class Applied(
        val replayed: Boolean,
        val hasPendingConsequences: Boolean,
    ) : EntryMigrationHostTransitionResult

    data object Conflict : EntryMigrationHostTransitionResult

    data class OperationalFailure(
        val retryable: Boolean,
    ) : EntryMigrationHostTransitionResult
}
