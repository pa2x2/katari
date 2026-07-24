package mihon.entry.interactions

import mihon.entry.interactions.host.tracking.EntryTrackingBackupHost
import tachiyomi.domain.entry.model.Entry

internal class DefaultEntryTrackingBackupFeature(
    private val host: EntryTrackingBackupHost,
) : EntryTrackingBackupFeature {
    override suspend fun snapshot(profileId: Long, entry: Entry): EntryTrackingBackupState? {
        return host.snapshot(profileId, entry.id)
            .takeIf(List<EntryTrackingBackupRecord>::isNotEmpty)
            ?.let(::EntryTrackingBackupState)
    }

    override suspend fun restore(profileId: Long, entry: Entry, state: EntryTrackingBackupState) {
        host.restore(profileId, entry.id, state.records)
    }
}
