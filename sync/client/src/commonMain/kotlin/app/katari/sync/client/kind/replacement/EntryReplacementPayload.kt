package app.katari.sync.client.kind.replacement

import app.katari.sync.client.payload.Stamped
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * "Entry [from] was replaced by another", so other devices move the entry with what hangs off it instead of keeping
 * the old one or deleting and recreating it.
 */
@Serializable
data class EntryReplacementPayload(
    @ProtoNumber(1) val from: String,
    @ProtoNumber(2) val replacement: Stamped<Replacement>,
)

@Serializable
data class Replacement(
    @ProtoNumber(1) val to: String,
    @ProtoNumber(2) val cause: Cause,
) {
    @Serializable
    enum class Cause {
        /** A source migration that replaced the entry; one that copied it leaves the old entry in place. */
        @ProtoNumber(1)
        MIGRATION,

        /** Moved to another profile, which changes its key. */
        @ProtoNumber(2)
        PROFILE_MOVE,
    }
}
