package app.katari.sync.client.kind.question

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.apply.LocalWrites
import app.katari.sync.client.kind.entry.EntryKind
import app.katari.sync.client.kind.entry.EntryPayload
import app.katari.sync.client.kind.profile.ProfileKind
import app.katari.sync.client.kind.profile.profileOf
import app.katari.sync.client.kind.profile.profileRecords
import app.katari.sync.client.kind.profile.restoreDeletedProfile
import app.katari.sync.client.kind.replacement.EntryReplacementKind
import app.katari.sync.client.kind.replacement.EntryReplacementPayload
import app.katari.sync.client.merge.RecordState
import app.katari.sync.client.merge.newestEdit
import app.katari.sync.client.payload.Stamped
import app.katari.sync.client.record.SyncedKind
import app.katari.sync.data.Sync_records
import app.katari.sync.protocol.clock.HybridTimestamp

/**
 * Notices, while a pulled record is merged, the conflicts that come to the person (D018), and raises the question
 * about each. The merge itself still applies its result, the provisional answer every device agrees on, except for a
 * profile's deletion, which leaves the profile in place until answered.
 *
 * A question already answered after both conflicting changes is not raised again, so old copies arriving late don't
 * reopen it.
 */
internal class QuestionDetection(private val writes: LocalWrites) {

    /** Checks [incoming] against [stored], this device's copy of the record [key], before they are merged. */
    suspend fun <P : Any> detect(kind: SyncedKind<P>, key: String, stored: Sync_records?, incoming: RecordState) {
        val local = stored?.let { RecordState.of(it.deleted, it.payload) }
        when {
            kind === EntryReplacementKind && local is RecordState.Live && incoming is RecordState.Live ->
                migrations(
                    stored,
                    EntryReplacementKind.decode(local.bytes),
                    EntryReplacementKind.decode(incoming.bytes),
                )
            kind === EntryKind && local is RecordState.Live && incoming is RecordState.Live ->
                notes(key, stored, EntryKind.decode(local.bytes), EntryKind.decode(incoming.bytes))
            kind === ProfileKind && incoming is RecordState.Deleted ->
                profileDeletedHere(ProfileKind.decodeUuid(key, local), incoming.tombstone.deletedAt)
            incoming is RecordState.Live -> changeInDeletedProfile(kind, kind.decode(incoming.bytes))
        }
    }

    /** The entry migrated here and elsewhere, since the copy both were based on, to different entries. */
    private suspend fun migrations(
        stored: Sync_records?,
        local: EntryReplacementPayload,
        incoming: EntryReplacementPayload,
    ) {
        val (a, b) = local.replacement to incoming.replacement
        if (a.value.to == b.value.to || a.at == b.at) return
        val based = stored?.base_payload?.let { EntryReplacementKind.decode(it).replacement.at } ?: Stamped.UNKNOWN_EDIT
        if (a.at == based || b.at == based) return
        val key = MigrationQuestionKind.key(local.from)
        val existing = stored(MigrationQuestionKind, key)
        if (existing != null && existing.answer.at > maxOf(a.at, b.at)) return
        val targets = listOf(MigrationTarget(a.value.to, a.at), MigrationTarget(b.value.to, b.at)).sortedBy { it.to }
        if (existing != null && MigrationQuestionKind.isOpen(existing) && existing.targets.value == targets) return
        writes.change(
            MigrationQuestionKind,
            key,
            MigrationQuestionPayload(
                from = local.from,
                targets = Stamped(targets, writes.newEdit()),
                answer = existing?.answer ?: Stamped(null, Stamped.UNKNOWN_EDIT),
            ),
        )
    }

    /** Notes changed here and elsewhere since the copy both were based on, to different text. */
    private suspend fun notes(
        key: String,
        stored: Sync_records?,
        local: EntryPayload,
        incoming: EntryPayload,
    ) {
        val (a, b) = local.notes to incoming.notes
        if (a.value == b.value || a.at == b.at) return
        val based = stored?.base_payload?.let { EntryKind.decode(it).notes.at } ?: Stamped.UNKNOWN_EDIT
        if (a.at == based || b.at == based) return
        val questionKey = NotesQuestionKind.key(key)
        val existing = stored(NotesQuestionKind, questionKey)
        if (existing != null && existing.answer.at > maxOf(a.at, b.at)) return
        val versions = listOf(NotesVersion(a.value, a.at), NotesVersion(b.value, b.at)).sortedBy { it.at }
        if (existing != null && NotesQuestionKind.isOpen(existing) && existing.versions.value == versions) return
        writes.change(
            NotesQuestionKind,
            questionKey,
            NotesQuestionPayload(
                entryKey = key,
                versions = Stamped(versions, writes.newEdit()),
                answer = existing?.answer ?: Stamped(null, Stamped.UNKNOWN_EDIT),
            ),
        )
    }

    /** A deletion arriving for a profile this device has changed things in since. */
    private suspend fun profileDeletedHere(uuid: String?, deletedAt: HybridTimestamp) {
        uuid ?: return
        val edited = profileRecords(writes.database, uuid).mapNotNull { it.edited }.maxOrNull() ?: return
        if (edited > deletedAt) raiseProfileDeletion(uuid, deletedAt, edited)
    }

    /** A change arriving for something in a profile already deleted, made after the deletion. */
    private suspend fun <P : Any> changeInDeletedProfile(kind: SyncedKind<P>, payload: P) {
        val uuid = profileOf(payload) ?: kind.entryKeyOf(payload)?.let { entryKey ->
            writes.database.sync_recordsQueries.getByKey(entryKey).awaitAsOneOrNull()
                ?.takeUnless { it.deleted }
                ?.let { EntryKind.decode(it.payload).profileUuid }
        } ?: return
        val profile = writes.database.sync_recordsQueries.getByKey(ProfileKind.key(uuid)).awaitAsOneOrNull() ?: return
        val deletion = RecordState.of(profile.deleted, profile.payload) as? RecordState.Deleted ?: return
        val edited = newestEdit(kind.serializer, payload) ?: return
        if (edited > deletion.tombstone.deletedAt) {
            raiseProfileDeletion(uuid, deletion.tombstone.deletedAt, edited)
            restoreDeletedProfile(writes, uuid)
        }
    }

    private suspend fun raiseProfileDeletion(uuid: String, deletedAt: HybridTimestamp, edited: HybridTimestamp) {
        val key = ProfileDeletionQuestionKind.key(uuid)
        val existing = stored(ProfileDeletionQuestionKind, key)
        if (existing != null && existing.answer.at > edited) return
        if (existing != null && ProfileDeletionQuestionKind.isOpen(existing)) return
        writes.change(
            ProfileDeletionQuestionKind,
            key,
            ProfileDeletionQuestionPayload(
                profileUuid = uuid,
                conflict = Stamped(ProfileDeletion(deletedAt, edited), writes.newEdit()),
                answer = existing?.answer ?: Stamped(null, Stamped.UNKNOWN_EDIT),
            ),
        )
    }

    private suspend fun <P : Any> stored(kind: QuestionKind<P>, key: String): P? =
        writes.database.sync_recordsQueries.getByKey(key).awaitAsOneOrNull()?.let { kind.decode(it.payload) }
}

/** The uuid of the profile a profile record is of, read from this device's copy of it. */
private fun ProfileKind.decodeUuid(key: String, local: RecordState?): String? = when (local) {
    is RecordState.Live -> decode(local.bytes).uuid
    is RecordState.Deleted -> local.tombstone.last.takeIf { it.isNotEmpty() }?.let { decode(it).uuid }
    null -> null
}?.takeIf { key(it) == key }
