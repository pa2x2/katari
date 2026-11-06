package app.katari.sync.client.kind.question

import app.katari.sync.client.payload.Stamped
import app.katari.sync.protocol.clock.HybridTimestamp
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * Questions are records like any other, so every device shows the same ones and an answer on any device settles them
 * everywhere (D019). Each holds what happened, stamped when it was noticed, and the answer, stamped when it was given:
 * a question is open while its answer is older than what it asks about, so a later conflict about the same thing asks
 * again and an answer never settles one raised after it.
 */
@Serializable
data class MigrationQuestionPayload(
    /** The entry that was migrated. */
    @ProtoNumber(1) val from: String,
    /** The entries it was migrated to, on different devices. */
    @ProtoNumber(2) val targets: Stamped<List<MigrationTarget>>,
    /** The target kept, or null before an answer. */
    @ProtoNumber(3) val answer: Stamped<String?>,
)

@Serializable
data class MigrationTarget(
    @ProtoNumber(1) val to: String,
    /** When the entry was migrated there, which names the device that did. */
    @ProtoNumber(2) val at: HybridTimestamp,
)

@Serializable
data class NotesQuestionPayload(
    @ProtoNumber(1) val entryKey: String,
    /** The notes as each device wrote them without seeing the other's. */
    @ProtoNumber(2) val versions: Stamped<List<NotesVersion>>,
    /** The notes kept, or null before an answer. */
    @ProtoNumber(3) val answer: Stamped<String?>,
)

@Serializable
data class NotesVersion(
    @ProtoNumber(1) val notes: String,
    @ProtoNumber(2) val at: HybridTimestamp,
)

@Serializable
data class ProfileDeletionQuestionPayload(
    @ProtoNumber(1) val profileUuid: String,
    @ProtoNumber(2) val conflict: Stamped<ProfileDeletion>,
    /** Null before an answer. */
    @ProtoNumber(3) val answer: Stamped<ProfileDeletionAnswer?>,
)

/** A profile deleted on one device while another device changed things in it after that. */
@Serializable
data class ProfileDeletion(
    @ProtoNumber(1) val deletedAt: HybridTimestamp,
    /** The newest change made in the profile, after [deletedAt]. */
    @ProtoNumber(2) val editedAt: HybridTimestamp,
)

@Serializable
enum class ProfileDeletionAnswer {
    @ProtoNumber(1)
    KEEP,

    @ProtoNumber(2)
    DELETE,
}
