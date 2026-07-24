package mihon.entry.interactions.host

import mihon.entry.interactions.EntryMergeMigrationReplacementResult
import mihon.entry.interactions.EntryMigrationMode
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.track.model.EntryTrack

/** Explicit-profile application boundary for Migration execution preparation and its owned database transition. */
interface EntryMigrationExecutionHost {
    fun profile(profileId: Long): EntryMigrationExecutionProfileHost
}

interface EntryMigrationExecutionProfileHost {
    suspend fun replay(operation: EntryMigrationHostOperation): EntryMigrationHostReplayResult

    suspend fun inspectExecution(
        sourceEntryId: Long,
        targetEntryId: Long,
    ): EntryMigrationExecutionInspectionResult

    suspend fun applyTransition(
        transition: EntryMigrationHostTransition,
        participateMergeReplacement: (suspend () -> EntryMergeMigrationReplacementResult)?,
    ): EntryMigrationHostTransitionResult
}

data class EntryMigrationHostOperation(
    val operationId: String,
    val intentFingerprint: String,
    val sourceEntryId: Long,
    val targetEntryId: Long,
    val mode: EntryMigrationMode,
)

sealed interface EntryMigrationHostReplayResult {
    data object NotApplied : EntryMigrationHostReplayResult

    data class Applied(
        val hasPendingConsequences: Boolean,
    ) : EntryMigrationHostReplayResult

    data object Conflict : EntryMigrationHostReplayResult

    data class OperationalFailure(
        val retryable: Boolean,
    ) : EntryMigrationHostReplayResult
}

sealed interface EntryMigrationExecutionInspectionResult {
    data class Ready(
        val source: Entry,
        val target: Entry,
        val sourceChildren: List<EntryChapter>,
        val targetChildren: List<EntryChapter>,
        val sourceCategoryIds: List<Long>,
        val sourceTracks: List<EntryTrack>,
    ) : EntryMigrationExecutionInspectionResult

    data object SourceMissing : EntryMigrationExecutionInspectionResult
    data object TargetMissing : EntryMigrationExecutionInspectionResult

    data class OperationalFailure(
        val retryable: Boolean,
    ) : EntryMigrationExecutionInspectionResult
}
