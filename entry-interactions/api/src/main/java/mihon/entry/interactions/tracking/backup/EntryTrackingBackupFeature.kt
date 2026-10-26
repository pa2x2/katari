package mihon.entry.interactions.tracking.backup

import tachiyomi.domain.entry.model.Entry

interface EntryTrackingBackupFeature {
    /** The tracking state of each of [entries] that is tracked, by entry id. */
    suspend fun snapshot(profileId: Long, entries: List<Entry>): Map<Long, EntryTrackingBackupState>

    suspend fun restore(profileId: Long, entry: Entry, state: EntryTrackingBackupState)
}
