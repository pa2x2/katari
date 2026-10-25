package mihon.entry.interactions.merge

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.serialization.Serializable
import tachiyomi.domain.entry.model.Entry

const val ENTRY_MERGE_BACKUP_STATE_ID = "entry.merge.backup"
const val ENTRY_MERGE_BACKUP_SCHEMA_VERSION = 1

interface EntryMergeBackupFeature {
    /** The members, target included, of every merge one of [entries] belongs to that [entries] leave out. */
    suspend fun companionsForBackup(profileId: Long, entries: List<Entry>): List<Entry>

    suspend fun snapshotForBackup(subject: EntryMergeSubject): EntryMergeBackupMember?

    suspend fun restore(
        destinationProfileId: Long,
        groups: List<EntryMergeBackupGroup>,
    ): EntryMergeBackupRestoreResult
}

/** The only Merge facts one Entry needs to serialize its existing wire-compatible backup fields. */
@Serializable
data class EntryMergeBackupMember(
    val target: EntryMergeBackupIdentity,
    val position: Int,
)

@Serializable
data class EntryMergeBackupIdentity(
    val sourceId: Long,
    val url: String,
    val type: EntryType,
)

data class EntryMergeBackupGroup(
    val target: EntryMergeBackupIdentity,
    val members: List<EntryMergeBackupGroupMember>,
)

data class EntryMergeBackupGroupMember(
    val identity: EntryMergeBackupIdentity,
    val position: Int,
)

data class EntryMergeBackupRestoreResult(
    val appliedGroupCount: Int,
    val skippedGroups: List<EntryMergeBackupSkippedGroup>,
)

data class EntryMergeBackupSkippedGroup(
    val target: EntryMergeBackupIdentity,
    val reason: EntryMergeBackupSkipReason,
)

enum class EntryMergeBackupSkipReason {
    MIXED_ENTRY_TYPES,
    MISSING_TARGET,
    INSUFFICIENT_MEMBERS,
    CONFLICT,
    OPERATIONAL_FAILURE,
}
