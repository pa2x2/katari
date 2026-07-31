package mihon.entry.interactions.tracking.host

import mihon.entry.interactions.tracking.backup.EntryTrackingBackupRecord

interface EntryTrackingBackupHost {
    suspend fun snapshot(profileId: Long, entryId: Long): List<EntryTrackingBackupRecord>

    suspend fun restore(profileId: Long, entryId: Long, records: List<EntryTrackingBackupRecord>)

    data object Empty : EntryTrackingBackupHost {
        override suspend fun snapshot(profileId: Long, entryId: Long): List<EntryTrackingBackupRecord> = emptyList()

        override suspend fun restore(profileId: Long, entryId: Long, records: List<EntryTrackingBackupRecord>) = Unit
    }
}
