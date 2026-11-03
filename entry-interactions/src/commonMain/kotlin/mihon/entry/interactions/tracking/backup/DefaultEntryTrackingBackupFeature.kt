package mihon.entry.interactions.tracking.backup

import mihon.entry.interactions.tracking.host.EntryTrackingBackupHost
import tachiyomi.domain.entry.model.Entry

internal class DefaultEntryTrackingBackupFeature(
    private val host: EntryTrackingBackupHost,
) : EntryTrackingBackupFeature {
    override suspend fun snapshot(profileId: Long, entries: List<Entry>): Map<Long, EntryTrackingBackupState> {
        return host.snapshot(profileId, entries.map(Entry::id))
            .filterValues(List<EntryTrackingBackupRecord>::isNotEmpty)
            .mapValues { (_, records) -> EntryTrackingBackupState(records) }
    }

    override suspend fun restore(profileId: Long, entry: Entry, state: EntryTrackingBackupState) {
        host.restore(profileId, entry.id, state.records)
    }
}
