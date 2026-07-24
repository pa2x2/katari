package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.serialization.Serializable
import tachiyomi.domain.entry.model.Entry

/** Feature-owned boundary for playback-preference backup and transfer. */
interface EntryPlaybackPreferencesFeature {
    fun isApplicable(type: EntryType): Boolean

    suspend fun snapshot(entry: Entry): EntryPlaybackPreferencesSnapshotResult

    suspend fun restore(
        entry: Entry,
        snapshot: EntryPlaybackPreferencesSnapshot,
    ): EntryPlaybackPreferencesRestoreResult

    suspend fun copy(
        sourceEntry: Entry,
        targetEntry: Entry,
    ): EntryPlaybackPreferencesCopyResult

    /** Captures a target-ready value so durable Migration retry never rereads the source. */
    suspend fun prepareMigration(
        sourceEntry: Entry,
        targetEntry: Entry,
    ): EntryPlaybackPreferencesMigrationPreparation

    suspend fun applyMigration(
        payload: EntryPlaybackPreferencesMigrationPayload,
    ): EntryPlaybackPreferencesRestoreResult
}

@Serializable
data class EntryPlaybackPreferencesMigrationPayload(
    val target: Entry,
    val snapshot: EntryPlaybackPreferencesSnapshot,
)

sealed interface EntryPlaybackPreferencesMigrationPreparation {
    data class Prepared(
        val payload: EntryPlaybackPreferencesMigrationPayload,
    ) : EntryPlaybackPreferencesMigrationPreparation
    data object NoPreferences : EntryPlaybackPreferencesMigrationPreparation
    data class Inapplicable(val types: Set<EntryType>) : EntryPlaybackPreferencesMigrationPreparation
    data class TypeMismatch(val sourceType: EntryType, val targetType: EntryType) :
        EntryPlaybackPreferencesMigrationPreparation
}

sealed interface EntryPlaybackPreferencesSnapshotResult {
    data class Captured(val snapshot: EntryPlaybackPreferencesSnapshot) : EntryPlaybackPreferencesSnapshotResult

    data object NoPreferences : EntryPlaybackPreferencesSnapshotResult

    data class Inapplicable(val type: EntryType) : EntryPlaybackPreferencesSnapshotResult
}

sealed interface EntryPlaybackPreferencesRestoreResult {
    data object Applied : EntryPlaybackPreferencesRestoreResult

    data class Inapplicable(val type: EntryType) : EntryPlaybackPreferencesRestoreResult
}

sealed interface EntryPlaybackPreferencesCopyResult {
    data object Copied : EntryPlaybackPreferencesCopyResult

    data object NoPreferences : EntryPlaybackPreferencesCopyResult

    data class Inapplicable(val types: Set<EntryType>) : EntryPlaybackPreferencesCopyResult

    data class TypeMismatch(
        val sourceType: EntryType,
        val targetType: EntryType,
    ) : EntryPlaybackPreferencesCopyResult
}
