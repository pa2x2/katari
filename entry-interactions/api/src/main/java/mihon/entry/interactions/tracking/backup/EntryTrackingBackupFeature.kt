package mihon.entry.interactions

import tachiyomi.domain.entry.model.Entry

interface EntryTrackingBackupFeature {
    suspend fun snapshot(profileId: Long, entry: Entry): EntryTrackingBackupState?

    suspend fun restore(profileId: Long, entry: Entry, state: EntryTrackingBackupState)
}
