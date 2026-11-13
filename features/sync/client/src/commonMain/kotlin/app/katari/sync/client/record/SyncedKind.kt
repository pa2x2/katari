package app.katari.sync.client.record

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.apply.ApplyOutcome
import app.katari.sync.client.apply.LocalWrites
import app.katari.sync.client.capture.CaptureTrigger
import app.katari.sync.client.part.ChoiceSet
import app.katari.sync.client.part.DataGroup
import app.katari.sync.client.part.ViewerSettingPreferences
import app.katari.sync.client.payload.UnknownFields
import app.katari.sync.client.setting.scope.PreferenceScopes
import app.katari.sync.client.setting.scope.SettingsArea
import app.katari.sync.protocol.file.FileReference
import kotlinx.serialization.KSerializer
import kotlinx.serialization.protobuf.ProtoBuf

/**
 * One kind of synced record: its name and format version as the envelope carries them, its payload, which writes
 * capture it, and how a local row becomes its payload.
 *
 * [name] and the payload's field numbers are a compatibility boundary. A field is added under an unused number; a
 * field that changes meaning, or is removed, makes a new [version], which older apps hold without applying. A version
 * whose payload still decodes the older ones' records reads them from [oldestReadableVersion] on; the records this
 * device stored under them are read again from their rows ([app.katari.sync.client.outbox.OutboxDrain]), so what the
 * new version keeps or reads differently reaches the server.
 */
abstract class SyncedKind<P : Any>(
    val name: String,
    val version: Int,
    val serializer: KSerializer<P>,
    /** What a record becomes once its local row is gone. */
    val absence: Absence,
) {
    internal abstract val capture: List<CaptureTrigger>

    internal open val oldestReadableVersion: Int get() = version

    fun reads(version: Int): Boolean = version in oldestReadableVersion..this.version

    internal abstract suspend fun read(reads: LocalReads, ref: LocalRef): LocalRead<P>

    /**
     * Writes to the row [ref] locates what its record holds and the row lost, when the row was deleted with its owner,
     * which leaves its record ([CaptureTrigger.owners]), and made again since. The drain does it before it first reads
     * a row it drains an edit of, so the edit adds to the record rather than starting it over. A restore doesn't: the
     * rows it writes hold the backup's values whole.
     */
    internal open suspend fun rejoin(reads: LocalReads, ref: LocalRef) = Unit

    /**
     * The payload holding both [local]'s and [incoming]'s edits of one record (D017). Every device must reach the same
     * payload from the same edits whatever order they come in, so it is commutative, associative and idempotent. The
     * fields a key is computed from are equal in both.
     */
    internal abstract fun merge(local: P, incoming: P): P

    /**
     * Writes [payload], the record [key] as merged with another device's copy, to this device's rows: inserting the
     * row when it isn't here, updating the synced columns when it is. Only sync writes this way, never capturing it
     * as an edit ([app.katari.sync.client.apply.IncomingRecords]).
     */
    internal abstract suspend fun apply(writes: LocalWrites, key: String, payload: P): ApplyOutcome

    /**
     * Removes the row [ref] locates, null when none here holds the record [key] another device deleted. Only kinds
     * whose records are deleted ([Absence.TOMBSTONE]) remove anything.
     */
    internal open suspend fun applyDeletion(writes: LocalWrites, key: String, ref: LocalRef?): ApplyOutcome =
        ApplyOutcome.Applied(null)

    /**
     * [edits], made to a record before another device deleted it in favour of [replacement]
     * ([app.katari.sync.client.payload.Tombstone.replacedBy]) as its row moved to that one's key, as edits of that one,
     * with the fields its key is computed from. Null for a kind whose records are only ever replaced by merging into
     * another row, whose edits go with the deletion.
     */
    internal open val moveInto: ((edits: P, replacement: P) -> P)? = null

    /**
     * Whether [payload], a live copy of a record deleted in favour of [replacement] as its row moved, holds edits made
     * where the row was by a device that hadn't heard of the move, rather than the row moved back there. False for a
     * kind that can't tell them apart: such a copy stays live until the device that made it hears of the move.
     */
    internal open fun editedBeforeHeardOfMove(payload: P, replacement: P): Boolean = false

    /**
     * The live record that holds the row [ref] under a key other than [key], the row's now: the record the row was
     * synced as before a change of what its key is computed from moved it, with its payload as stored.
     */
    internal suspend fun heldBeforeMove(reads: LocalReads, ref: LocalRef, key: String): HeldRecord<P>? {
        val held = reads.database.sync_recordsQueries.getByLocalRef(name, ref.json).awaitAsOneOrNull()
        if (held == null || held.deleted || held.record_key == key) return null
        return HeldRecord(held.record_key, decode(held.payload))
    }

    /** The entry a record of this kind belongs to, so records of an entry another one replaced follow it there. */
    internal open fun entryKeyOf(payload: P): String? = null

    /** The data group whose switch this kind follows; null for one that always syncs, or follows something else. */
    internal open val dataGroup: DataGroup? = null

    /** Whether this device syncs [payload] by what it chose ([ChoiceSet]), its profile aside. */
    internal open fun syncsOn(choices: ChoiceSet, payload: P): Boolean = dataGroup?.let(choices::syncs) ?: true

    /**
     * The key of the app setting a record of this kind holds a title's own value of, whose sync choice it follows
     * besides [syncsOn]; null for a record that stands in for no setting.
     */
    internal open fun overriddenSetting(payload: P, viewerSettings: ViewerSettingPreferences): String? = null

    /** The uuid of the profile a record of this kind is in, when its payload names one rather than an entry. */
    internal open fun profileOf(payload: P): String? = null

    /** The key of the app preference a setting record holds; null for a record that holds none. */
    internal open fun settingKey(payload: P): String? = null

    /** The Settings area of a setting record; null for a record that is no setting. */
    internal open fun settingsArea(payload: P): SettingsArea? = settingKey(payload)?.let(PreferenceScopes::markOf)?.area

    /**
     * The files [payload] names, which the envelope lists so the server keeps them while the record is live
     * ([app.katari.sync.protocol.record.RecordEnvelope.files]).
     */
    internal open fun files(payload: P): List<FileReference> = emptyList()

    /** For [Absence.REMOVED_FLAG]: [previous] marked removed. */
    internal open fun removed(previous: P, stamps: FieldStamps): P =
        throw UnsupportedOperationException("$name records are not memberships")

    private val fieldNumbers: Set<Int> by lazy { UnknownFields.numbersOf(serializer.descriptor) }

    fun decode(payload: ByteArray): P = ProtoBuf.decodeFromByteArray(serializer, payload)

    /** [payload], followed by the fields of [previous] that this version doesn't know (see [UnknownFields]). */
    fun encode(payload: P, previous: ByteArray?): ByteArray {
        val known = ProtoBuf.encodeToByteArray(serializer, payload)
        return if (previous == null) known else known + UnknownFields.of(previous, fieldNumbers)
    }

    enum class Absence {
        /** Deleted, as a tombstone envelope. */
        TOMBSTONE,

        /** Kept, with its removed flag set: one record per member of a set, so adding and removing both merge. */
        REMOVED_FLAG,

        /** Kept as last synced: the row went away for a reason that is not the person's intent, like a refresh. */
        KEEP,
    }
}

/** The record [key] as stored, with its [payload]. */
class HeldRecord<P : Any>(val key: String, val payload: P)
