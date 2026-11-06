package app.katari.sync.client.kind.replacement

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.capture.CaptureTrigger
import app.katari.sync.client.capture.CaptureTrigger.Event
import app.katari.sync.client.capture.CaptureTrigger.Field
import app.katari.sync.client.identity.RecordKeys
import app.katari.sync.client.kind.entry.EntryKind
import app.katari.sync.client.record.LocalRead
import app.katari.sync.client.record.LocalReads
import app.katari.sync.client.record.LocalRef
import app.katari.sync.client.record.RecordRef
import app.katari.sync.client.record.SyncedKind

/**
 * Entry replacements, keyed by the replaced entry's key. Local ref: `["migration", operation_id]` for a source
 * migration in replace mode, or `["move", entry_id, old profile uuid]` for a move to another profile, which keeps the
 * row and changes its profile.
 */
object EntryReplacementKind : SyncedKind<EntryReplacementPayload>(
    "entry.replacement",
    1,
    EntryReplacementPayload.serializer(),
    Absence.KEEP,
) {
    private const val MIGRATION = "migration"
    private const val MOVE = "move"

    override val capture = listOf(
        CaptureTrigger(
            "entry_migration_operations",
            Event.INSERT,
            listOf("'$MIGRATION'", "NEW.operation_id"),
            condition = "NEW.mode = 'REPLACE'",
        ),
        CaptureTrigger(
            "entries",
            Event.UPDATE,
            listOf("'$MOVE'", "NEW._id", "(SELECT uuid FROM profiles WHERE _id = OLD.profile_id)"),
            fields = listOf(Field(Field.EVERY, "profile_id")),
        ),
    )

    override suspend fun read(reads: LocalReads, ref: LocalRef): LocalRead<EntryReplacementPayload> = when (
        ref.text(
            0,
        )
    ) {
        MIGRATION -> {
            val operation = reads.database.sync_local_rowsQueries.entryMigrationRow(ref.text(1)).awaitAsOneOrNull()
            if (operation == null) {
                LocalRead.Missing
            } else {
                found(
                    reads,
                    from = reads.entryKey(operation.source_entry_id),
                    toEntryId = operation.target_entry_id,
                    cause = Replacement.Cause.MIGRATION,
                    parents = listOf(operation.source_entry_id, operation.target_entry_id),
                )
            }
        }
        MOVE -> {
            val entry = reads.entry(ref.long(1))
            if (entry == null) {
                LocalRead.Missing
            } else {
                found(
                    reads,
                    from = reads.entryKey(ref.text(2), entry.id_scheme, entry.external_id, entry.url, entry.type),
                    toEntryId = entry._id,
                    cause = Replacement.Cause.PROFILE_MOVE,
                    parents = listOf(entry._id),
                )
            }
        }
        else -> LocalRead.Missing
    }

    private suspend fun found(
        reads: LocalReads,
        from: String?,
        toEntryId: Long,
        cause: Replacement.Cause,
        parents: List<Long>,
    ): LocalRead<EntryReplacementPayload> {
        val to = reads.entryKey(toEntryId)
        if (from == null || to == null) return LocalRead.NotSynced
        return LocalRead.Found(
            key = RecordKeys.derive(name, from),
            parents = parents.map { RecordRef(EntryKind, EntryKind.ref(it)) },
        ) { previous, stamps ->
            EntryReplacementPayload(from, stamps.field(2, Replacement(to, cause), previous?.replacement))
        }
    }
}
