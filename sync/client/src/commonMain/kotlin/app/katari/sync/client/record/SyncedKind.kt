package app.katari.sync.client.record

import app.katari.sync.client.capture.CaptureTrigger
import app.katari.sync.client.payload.UnknownFields
import kotlinx.serialization.KSerializer
import kotlinx.serialization.protobuf.ProtoBuf

/**
 * One kind of synced record: its name and format version as the envelope carries them, its payload, which writes
 * capture it, and how a local row becomes its payload.
 *
 * [name] and the payload's field numbers are a compatibility boundary. A field is added under an unused number; a
 * field that changes meaning, or is removed, makes a new [version], which older apps hold without applying.
 */
abstract class SyncedKind<P : Any>(
    val name: String,
    val version: Int,
    val serializer: KSerializer<P>,
    /** What a record becomes once its local row is gone. */
    val absence: Absence,
) {
    internal abstract val capture: List<CaptureTrigger>

    internal abstract suspend fun read(reads: LocalReads, ref: LocalRef): LocalRead<P>

    /**
     * The payload holding both [local]'s and [incoming]'s edits of one record (D017). Every device must reach the same
     * payload from the same edits whatever order they come in, so it is commutative, associative and idempotent. The
     * fields a key is computed from are equal in both.
     */
    internal abstract fun merge(local: P, incoming: P): P

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
