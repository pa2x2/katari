package mihon.entry.interactions.tracking.host

import mihon.entry.interactions.tracking.backup.EntryTrackingBackupRecord

interface EntryTrackingBackupHost {
    /** The tracks of each of [entryIds] that has any, by entry id. */
    suspend fun snapshot(profileId: Long, entryIds: Collection<Long>): Map<Long, List<EntryTrackingBackupRecord>>

    suspend fun restore(profileId: Long, entryId: Long, records: List<EntryTrackingBackupRecord>)

    data object Empty : EntryTrackingBackupHost {
        override suspend fun snapshot(
            profileId: Long,
            entryIds: Collection<Long>,
        ): Map<Long, List<EntryTrackingBackupRecord>> = emptyMap()

        override suspend fun restore(profileId: Long, entryId: Long, records: List<EntryTrackingBackupRecord>) = Unit
    }
}
