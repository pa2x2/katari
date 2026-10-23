package mihon.entry.interactions.merge.backup

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.merge.EntryMergeBackupIdentity
import mihon.entry.interactions.persistence.backup.EntryBackupRestoreEvent
import mihon.entry.interactions.persistence.backup.EntryBackupRestoreFinalizingEvent
import mihon.entry.interactions.persistence.backup.EntryBackupRestoreIssue
import mihon.entry.interactions.persistence.backup.EntryBackupRestoreSessionStateKey
import mihon.entry.interactions.persistence.backup.EntryBackupSnapshotEvent
import mihon.entry.interactions.persistence.backup.EntryBackupStateParticipant
import mihon.entry.interactions.persistence.backup.decodeEntryBackupState
import mihon.entry.interactions.persistence.backup.entryBackupStateEnvelope

/** Listings restore once every listed title exists, so they queue during restore and apply when it ends. */
internal class EntryMergeSourceListingBackupParticipant(
    private val listings: () -> EntryMergeSourceListingBackup,
) : EntryBackupStateParticipant {
    override val stateId = ENTRY_MERGE_SOURCE_LISTING_BACKUP_STATE_ID

    override suspend fun snapshot(event: EntryBackupSnapshotEvent) {
        val state = listings().snapshot(event.profileId, event.entry.id) ?: return
        event.contributions.add(
            entryBackupStateEnvelope(
                stateId,
                ENTRY_MERGE_SOURCE_LISTING_BACKUP_SCHEMA_VERSION,
                EntryMergeSourceListingBackupState.serializer(),
                state,
            ),
        )
    }

    override suspend fun restore(event: EntryBackupRestoreEvent) {
        val state = event.states.decodeEntryBackupState(
            stateId,
            ENTRY_MERGE_SOURCE_LISTING_BACKUP_SCHEMA_VERSION,
            EntryMergeSourceListingBackupState.serializer(),
        ) ?: return
        val pending = event.session.state(SESSION_STATE_KEY, ::PendingListings).listings
        val root = EntryMergeBackupIdentity(event.entry.source, event.entry.url, event.entry.type)
        pending.getOrPut(event.profileId to event.entry.type, ::linkedMapOf)[root] = state.listing
    }

    override suspend fun finalizeRestore(event: EntryBackupRestoreFinalizingEvent) {
        val pending = event.session.stateOrNull(SESSION_STATE_KEY) ?: return
        val restored = pending.listings.remove(event.profileId to event.type).orEmpty()
        if (pending.listings.isEmpty()) event.session.remove(SESSION_STATE_KEY)
        if (restored.isEmpty()) return
        listings().restore(event.profileId, restored).forEach { root ->
            event.issues.add(
                EntryBackupRestoreIssue(
                    participantId = stateId,
                    description = "Skipped merged entry members of ${root.sourceId}:${root.url}",
                ),
            )
        }
    }
}

private class PendingListings(
    val listings:
    MutableMap<Pair<Long, EntryType>, LinkedHashMap<EntryMergeBackupIdentity, List<EntryMergeBackupIdentity>>> =
        linkedMapOf(),
)

private val SESSION_STATE_KEY = EntryBackupRestoreSessionStateKey<PendingListings>("entry.merge.source-listing.restore")
