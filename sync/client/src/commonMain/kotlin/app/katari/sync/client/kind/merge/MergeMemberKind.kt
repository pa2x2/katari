package app.katari.sync.client.kind.merge

import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.apply.ApplyOutcome
import app.katari.sync.client.apply.LocalWrites
import app.katari.sync.client.apply.idOr
import app.katari.sync.client.capture.CaptureTrigger
import app.katari.sync.client.capture.CaptureTrigger.Event
import app.katari.sync.client.capture.CaptureTrigger.Field
import app.katari.sync.client.capture.CaptureTrigger.Owner
import app.katari.sync.client.identity.RecordKeys
import app.katari.sync.client.kind.entry.EntryKind
import app.katari.sync.client.merge.latest
import app.katari.sync.client.record.FieldStamps
import app.katari.sync.client.record.LocalRead
import app.katari.sync.client.record.LocalReads
import app.katari.sync.client.record.LocalRef
import app.katari.sync.client.record.RecordRef
import app.katari.sync.client.record.SyncedKind

/**
 * Members of merges the person made, keyed by the member entry's key. Local ref: `[profile_id, entry_id]`. Groups a
 * source declared are derived on every device from the source and never sync.
 */
object MergeMemberKind : SyncedKind<MergeMemberPayload>(
    "merge.member",
    1,
    MergeMemberPayload.serializer(),
    Absence.REMOVED_FLAG,
) {
    override val capture = listOf(
        CaptureTrigger("merged_entries", Event.INSERT, listOf("NEW.profile_id", "NEW.entry_id")),
        CaptureTrigger(
            "merged_entries",
            Event.UPDATE,
            listOf("NEW.profile_id", "NEW.entry_id"),
            fields = listOf(Field(2, "target_entry_id"), Field(3, "position")),
        ),
        CaptureTrigger(
            "merged_entries",
            Event.DELETE,
            listOf("OLD.profile_id", "OLD.entry_id"),
            owners = listOf(Owner.entry(), Owner.entry("target_entry_id")),
        ),
    )

    override suspend fun read(reads: LocalReads, ref: LocalRef): LocalRead<MergeMemberPayload> {
        val row = reads.database.sync_local_rowsQueries.mergeMemberRow(ref.long(0), ref.long(1)).awaitAsOneOrNull()
            ?: return LocalRead.Missing
        val memberKey = reads.entryKey(row.entry_id) ?: return LocalRead.NotSynced
        val targetKey = reads.entryKey(row.target_entry_id) ?: return LocalRead.NotSynced
        return LocalRead.Found(
            key = RecordKeys.derive(name, memberKey),
            parents = listOf(
                RecordRef(EntryKind, EntryKind.ref(row.entry_id)),
                RecordRef(EntryKind, EntryKind.ref(row.target_entry_id)),
            ),
        ) { previous, stamps ->
            MergeMemberPayload(
                memberKey = memberKey,
                target = stamps.field(2, targetKey, previous?.target),
                position = stamps.field(3, row.position, previous?.position),
                removed = stamps.field(4, false, previous?.removed),
            )
        }
    }

    override fun removed(previous: MergeMemberPayload, stamps: FieldStamps) =
        previous.copy(removed = stamps.field(4, true, previous.removed))
    override fun merge(local: MergeMemberPayload, incoming: MergeMemberPayload) = local.copy(
        target = latest(local.target, incoming.target),
        position = latest(local.position, incoming.position),
        removed = latest(local.removed, incoming.removed),
    )

    override suspend fun apply(writes: LocalWrites, key: String, payload: MergeMemberPayload): ApplyOutcome {
        val entryId = writes.entryId(payload.memberKey).idOr { return it }
        val queries = writes.database.sync_applyQueries
        val profileId = queries.entryProfile(entryId).awaitAsOne()
        queries.deleteMergeMember(profileId, entryId)
        if (!payload.removed.value) {
            val targetId = writes.entryId(payload.target.value).idOr { return it }
            queries.insertMergeMember(profileId, targetId, entryId, payload.position.value)
        }
        return ApplyOutcome.Applied(LocalRef.of(profileId, entryId))
    }
}
