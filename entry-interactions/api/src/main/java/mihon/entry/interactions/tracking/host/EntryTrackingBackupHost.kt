package mihon.entry.interactions.host.tracking

import mihon.entry.interactions.EntryTrackingBackupRecord

interface EntryTrackingBackupHost {
    suspend fun snapshot(profileId: Long, entryId: Long): List<EntryTrackingBackupRecord>

    suspend fun restore(profileId: Long, entryId: Long, records: List<EntryTrackingBackupRecord>)

    data object Empty : EntryTrackingBackupHost {
        override suspend fun snapshot(profileId: Long, entryId: Long): List<EntryTrackingBackupRecord> = emptyList()

        override suspend fun restore(profileId: Long, entryId: Long, records: List<EntryTrackingBackupRecord>) = Unit
    }
}
