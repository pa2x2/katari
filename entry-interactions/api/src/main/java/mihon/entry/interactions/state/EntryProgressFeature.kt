package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.serialization.Serializable
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryProgressState

/** Feature-owned boundary for live progress persistence and portable state operations. */
interface EntryProgressFeature {
    fun isApplicable(type: EntryType): Boolean

    suspend fun recordMediaProgress(event: EntryMediaSessionEvent.Progressed): EntryProgressRecordingResult

    suspend fun snapshot(entry: Entry): EntryProgressSnapshotResult

    suspend fun restore(
        entry: Entry,
        snapshot: EntryProgressSnapshot,
    ): EntryProgressRestoreResult

    suspend fun copy(
        sourceEntry: Entry,
        targetEntry: Entry,
        resourceMappings: List<EntryProgressResourceMapping>,
    ): EntryProgressCopyResult

    /** Captures a target-ready value so durable Migration retry never rereads the source. */
    suspend fun prepareMigration(
        sourceEntry: Entry,
        targetEntry: Entry,
        resourceMappings: List<EntryProgressResourceMapping>,
    ): EntryProgressMigrationPreparation

    suspend fun applyMigration(payload: EntryProgressMigrationPayload): EntryProgressRestoreResult
}

data class EntryProgressRecordingResult(
    val state: EntryProgressState,
    val completedNow: Boolean,
)

@Serializable
data class EntryProgressMigrationPayload(
    val target: Entry,
    val snapshot: EntryProgressSnapshot,
)

sealed interface EntryProgressMigrationPreparation {
    data class Prepared(val payload: EntryProgressMigrationPayload) : EntryProgressMigrationPreparation
    data class Inapplicable(val types: Set<EntryType>) : EntryProgressMigrationPreparation
    data class IncompatibleTypes(
        val sourceType: EntryType,
        val targetType: EntryType,
    ) : EntryProgressMigrationPreparation
}

sealed interface EntryProgressSnapshotResult {
    data class Available(val snapshot: EntryProgressSnapshot) : EntryProgressSnapshotResult

    data class Inapplicable(val type: EntryType) : EntryProgressSnapshotResult
}

sealed interface EntryProgressRestoreResult {
    data object Applied : EntryProgressRestoreResult

    data class Inapplicable(val type: EntryType) : EntryProgressRestoreResult
}

sealed interface EntryProgressCopyResult {
    data object Applied : EntryProgressCopyResult

    data class Inapplicable(val types: Set<EntryType>) : EntryProgressCopyResult

    data class IncompatibleTypes(
        val sourceType: EntryType,
        val targetType: EntryType,
    ) : EntryProgressCopyResult
}
