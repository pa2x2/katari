package eu.kanade.tachiyomi.data.backup.models.compatibility

import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.data.backup.stream.BackupSummary
import mihon.entry.interactions.persistence.backup.EntryBackupStateCodec
import mihon.entry.interactions.tracking.backup.ENTRY_TRACKING_BACKUP_STATE_ID
import mihon.entry.interactions.tracking.backup.EntryTrackingBackupState

/*
 * Kitsu scores used to be stored halved (1–10) and are now kept on Kitsu's native 2–20 scale, which migration 53
 * applied to stored tracks. Restoring an older backup has to double its Kitsu scores the same way, or the next sync
 * would overwrite the user's rating on Kitsu with half of it.
 */

private const val KITSU_TRACKER_ID = 3L

internal fun BackupSummary.hasHalvedKitsuScores(): Boolean {
    // Mihon backups carry no version; only v0.21+, which switched to the native scale, writes an entry's next update.
    return !metadata.kitsuScoresNative && !mihonEntryHasNextUpdate
}

internal fun BackupEntry.doubleKitsuScores() {
    tracking = tracking.map { record ->
        if (record.syncId.toLong() == KITSU_TRACKER_ID && record.score > 0f) {
            record.copy(score = record.score * 2)
        } else {
            record
        }
    }
    featureStates = featureStates.map { state ->
        if (state.participantId != ENTRY_TRACKING_BACKUP_STATE_ID) return@map state
        val tracking = EntryBackupStateCodec.decode(EntryTrackingBackupState.serializer(), state.payload)
        val doubled = tracking.copy(
            records = tracking.records.map { record ->
                if (record.serviceId == KITSU_TRACKER_ID && record.score > 0.0) {
                    record.copy(score = record.score * 2)
                } else {
                    record
                }
            },
        )
        state.copy(payload = EntryBackupStateCodec.encode(EntryTrackingBackupState.serializer(), doubled))
    }
}
