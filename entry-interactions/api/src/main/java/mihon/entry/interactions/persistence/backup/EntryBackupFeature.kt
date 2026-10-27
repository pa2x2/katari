package mihon.entry.interactions.persistence.backup

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry

/** Feature-owned Entry state serialized without exposing its schema to backup orchestration. */
data class EntryFeatureStateEnvelope(
    val participantId: String,
    val schemaVersion: Int,
    val payload: ByteArray,
    /**
     * The value [payload] was encoded from, so in-process readers can skip decoding it. Null when the envelope was read
     * back from a backup file.
     */
    val state: Any? = null,
)

data class EntryBackupSelection(
    val includeContentState: Boolean,
    val includeTrackingState: Boolean,
)

interface EntryBackupFeature {
    /** [entries] and every entry a backup of them also needs so the state they share restores whole. */
    suspend fun withCompanionEntries(profileId: Long, entries: List<Entry>): List<Entry>

    /** The feature states of each of [entries] by entry id, read for all of them together. */
    suspend fun snapshot(
        profileId: Long,
        entries: List<Entry>,
        selection: EntryBackupSelection,
    ): Map<Long, List<EntryFeatureStateEnvelope>>

    suspend fun restore(
        session: EntryBackupRestoreSession,
        profileId: Long,
        entry: Entry,
        states: List<EntryFeatureStateEnvelope>,
    )

    /** Completes participants, such as Merge, that require every referenced Entry to exist first. */
    suspend fun finalizeRestore(
        session: EntryBackupRestoreSession,
        profileId: Long,
        restoredTypes: Set<EntryType>,
    ): EntryBackupRestoreFinalization
}

data class EntryBackupRestoreFinalization(
    val issues: List<EntryBackupRestoreIssue>,
)

data class EntryBackupRestoreIssue(
    val participantId: String,
    val description: String,
)
