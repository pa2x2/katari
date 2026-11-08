package app.katari.sync.client.kind.replacement

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.apply.ApplyOutcome
import app.katari.sync.client.apply.LocalWrites
import app.katari.sync.client.apply.idOr
import app.katari.sync.client.capture.CaptureTrigger
import app.katari.sync.client.capture.CaptureTrigger.Event
import app.katari.sync.client.capture.CaptureTrigger.Field
import app.katari.sync.client.file.local.FileOwner
import app.katari.sync.client.file.local.want
import app.katari.sync.client.identity.RecordKeys
import app.katari.sync.client.kind.entry.EntryKind
import app.katari.sync.client.kind.replacement.carry.ReplacedEntryEdits
import app.katari.sync.client.merge.latest
import app.katari.sync.client.payload.Stamped
import app.katari.sync.client.record.LocalRead
import app.katari.sync.client.record.LocalReads
import app.katari.sync.client.record.LocalRef
import app.katari.sync.client.record.RecordRef
import app.katari.sync.client.record.SyncedKind
import tachiyomi.domain.entry.migration.EntryMigrationFingerprint

/**
 * Entry replacements, keyed by the replaced entry's key. Local ref: `["migration", operation_id, ...]` for a source
 * migration in replace mode, followed by the target's library state when the operation was recorded, or
 * `["move", entry_id, old profile uuid]` for a move to another profile, which keeps the row and changes its profile.
 */
object EntryReplacementKind : SyncedKind<EntryReplacementPayload>(
    "entry.replacement",
    1,
    EntryReplacementPayload.serializer(),
    Absence.KEEP,
) {
    private const val MIGRATION = "migration"
    private const val MOVE = "move"

    /** The target's columns a migration's local ref ends with, in [LibraryState]'s order. */
    private val TARGET_STATE = listOf("favorite", "library_pinned", "chapter_flags", "date_added", "notes")

    private fun targetBefore(ref: LocalRef): LibraryState? {
        if (ref.size < 2 + TARGET_STATE.size) return null
        val favorite = ref.longOrNull(2) ?: return null
        return LibraryState(
            favorite = favorite == 1L,
            libraryPinned = ref.longOrNull(3) == 1L,
            chapterFlags = ref.longOrNull(4) ?: 0L,
            dateAdded = ref.longOrNull(5) ?: 0L,
            notes = ref.text(6),
        )
    }

    fun key(from: String): String = RecordKeys.derive(name, from)

    override val capture = listOf(
        // The target's library state as the migration found it: the app records the operation before changing entries
        CaptureTrigger(
            "entry_migration_operations",
            Event.INSERT,
            listOf("'$MIGRATION'", "NEW.operation_id") + TARGET_STATE.map { column ->
                "(SELECT $column FROM entries WHERE _id = NEW.target_entry_id)"
            },
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
                val inOrder = reads.database.sync_local_rowsQueries.entryMigrationChapterPositions(ref.text(1))
                    .awaitAsList()
                    .map { PairedChapter(it.source_chapter_url, it.target_chapter_url) }
                found(
                    reads,
                    from = reads.entryKey(operation.source_entry_id),
                    toEntryId = operation.target_entry_id,
                    cause = Replacement.Cause.MIGRATION,
                    parents = listOf(operation.source_entry_id, operation.target_entry_id),
                    options = EntryMigrationFingerprint.options(operation.intent_fingerprint).map { it.name }.sorted(),
                    // The target's library state is the merged entry's to change, so a question turning this down
                    // leaves it as the merged entry's migration left it
                    targetBefore = targetBefore(ref).takeUnless { operation.chapter_state_only },
                    chapters = MigratedChapters(inOrder, operation.chapter_state_only)
                        .takeUnless { it == MigratedChapters() },
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
        options: List<String> = emptyList(),
        targetBefore: LibraryState? = null,
        chapters: MigratedChapters? = null,
    ): LocalRead<EntryReplacementPayload> {
        val to = reads.entryKey(toEntryId)
        if (from == null || to == null) return LocalRead.NotSynced
        return LocalRead.Found(
            key = key(from),
            parents = parents.map { RecordRef(EntryKind, EntryKind.ref(it)) },
        ) { previous, stamps ->
            val replacement = stamps.field(2, Replacement(to, cause, options, targetBefore), previous?.replacement)
            EntryReplacementPayload(from, replacement, chapters?.let { Stamped(it, replacement.at) })
        }
    }

    /** The newer replacement, with how it carried chapters from whichever copy holds that for it. */
    override fun merge(local: EntryReplacementPayload, incoming: EntryReplacementPayload): EntryReplacementPayload {
        val replacement = latest(local.replacement, incoming.replacement)
        return local.copy(
            replacement = replacement,
            chapters = listOfNotNull(local.chapters, incoming.chapters).firstOrNull { it.at == replacement.at },
        )
    }

    /**
     * The replacement's own changes arrive as records of the new entry, made by the device that replaced it; here the
     * old entry's rows follow a move to another profile, or its activity follows a migration, as it did on that device,
     * and its edits made since carry over ([ReplacedEntryEdits]).
     * The old entry never returns to the library: its record no longer sets it as a favorite ([EntryKind.apply]).
     */
    override suspend fun apply(writes: LocalWrites, key: String, payload: EntryReplacementPayload): ApplyOutcome {
        val replacement = payload.replacement.value
        val toId = writes.entryId(replacement.to).idOr { return it }
        when (replacement.cause) {
            Replacement.Cause.PROFILE_MOVE -> moveRows(writes, payload.from, toId)
            Replacement.Cause.MIGRATION -> moveActivity(writes, payload.from, toId)
        }
        ReplacedEntryEdits(writes).carryAll(payload.from)
        return ApplyOutcome.Applied(null)
    }

    /**
     * Moves the old entry's chapters, history, positions and activity to the entry it became in the other profile,
     * then deletes the old row with what didn't move: its category links, as a local move drops them, what the new
     * entry already had, which its own records hold, and its custom cover, which the new entry's record names.
     */
    private suspend fun moveRows(writes: LocalWrites, from: String, toId: Long) {
        val fromId = otherLocalEntry(writes, from, toId) ?: return
        val queries = writes.database.sync_applyQueries
        val progressChapters = queries.progressChapterLinks(fromId).awaitAsList()
        queries.detachProgressChapters(fromId)
        queries.moveChapters(toEntryId = toId, fromEntryId = fromId)
        queries.moveProgress(toEntryId = toId, fromEntryId = fromId)
        progressChapters.forEach { queries.relinkProgressChapter(chapterId = it.chapter_id, progressId = it._id) }
        queries.moveHistory(toEntryId = toId, fromEntryId = fromId)
        queries.moveActivitySessions(toEntryId = toId, fromEntryId = fromId)
        queries.moveActivityCompletions(toEntryId = toId, fromEntryId = fromId)
        writes.database.want(FileOwner.CustomCover(fromId), null)
        queries.deleteEntry(fromId)
    }

    /**
     * Moves the old entry's sessions and completions to the entry that replaced it by a migration, as migrating moved
     * them on the device that migrated. Its history moved there to the new entry's chapters, whose records bring it.
     */
    private suspend fun moveActivity(writes: LocalWrites, from: String, toId: Long) {
        val fromId = otherLocalEntry(writes, from, toId) ?: return
        val queries = writes.database.sync_applyQueries
        queries.moveActivitySessions(toEntryId = toId, fromEntryId = fromId)
        queries.moveActivityCompletions(toEntryId = toId, fromEntryId = fromId)
    }

    /** The local ID of the entry [from], when its row is here and isn't the entry [toId] it became. */
    private suspend fun otherLocalEntry(writes: LocalWrites, from: String, toId: Long): Long? {
        val ref = writes.database.sync_recordsQueries.getByKey(from).awaitAsOneOrNull()?.local_ref ?: return null
        val fromId = LocalRef(ref).long(0)
        return fromId.takeIf { it != toId && writes.database.sync_applyQueries.entryExists(it).awaitAsOne() }
    }
}
