package mihon.entry.interactions

import kotlinx.serialization.Serializable

const val ENTRY_TRACKING_BACKUP_STATE_ID = "entry.tracking.backup"
const val ENTRY_TRACKING_BACKUP_SCHEMA_VERSION = 1

@Serializable
data class EntryTrackingBackupState(
    val records: List<EntryTrackingBackupRecord>,
)

@Serializable
data class EntryTrackingBackupRecord(
    val serviceId: Long,
    val remoteId: Long,
    val libraryId: Long?,
    val title: String,
    val progress: Double,
    val total: Long,
    val score: Double,
    val status: Long,
    val startDate: Long,
    val finishDate: Long,
    val remoteUrl: String,
    val private: Boolean,
)
