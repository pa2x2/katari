package app.katari.sync.protocol.error

import app.katari.sync.protocol.version.ProtocolVersionRange
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * Why the server refused a call. A refused record in a push is not an error but a
 * [app.katari.sync.protocol.push.PushOutcome.STALE] result, so the rest of the batch still goes through.
 */
@Serializable
data class SyncError(
    @ProtoNumber(1) val code: SyncErrorCode,
    /** For logs only; the app words what it shows from [code]. */
    @ProtoNumber(2) val message: String = "",
    /** Set with [SyncErrorCode.PROTOCOL_VERSION_UNSUPPORTED]. */
    @ProtoNumber(3) val supportedVersions: ProtocolVersionRange? = null,
    /**
     * Set with [SyncErrorCode.UNAUTHORIZED] when the sign-in was not checked because too many attempts failed from
     * this address or for this account: how long to wait before trying again, in milliseconds.
     */
    @ProtoNumber(4) val retryAfterMillis: Long = 0,
)

/**
 * The refusals an app reacts to differently. Every version of the protocol must be able to read
 * [PROTOCOL_VERSION_UNSUPPORTED] with its range, so this message and that entry keep their numbers forever.
 */
@Serializable
enum class SyncErrorCode {
    /**
     * Wrong password, an expired or used link code, or a token the server does not know. The person signs in again,
     * after [SyncError.retryAfterMillis] when it is set.
     */
    @ProtoNumber(1)
    UNAUTHORIZED,

    /** This device was removed from the account. It stops syncing and keeps all its data. */
    @ProtoNumber(2)
    DEVICE_REVOKED,

    /**
     * The device was away so long that the server no longer keeps what it needs for its position: deletion markers
     * it had not seen, or the history chain the position is checked against. It resyncs in full.
     */
    @ProtoNumber(3)
    CURSOR_EXPIRED,

    /**
     * The position is past anything this server has given out, or the server's history up to it is not the one the
     * device saw ([app.katari.sync.protocol.server.ServerIdentity.chain]): the server lost data or was restored. The
     * device resyncs in full.
     */
    @ProtoNumber(4)
    CURSOR_UNKNOWN,

    /** The app's protocol version is outside [SyncError.supportedVersions]; one side needs an update. */
    @ProtoNumber(5)
    PROTOCOL_VERSION_UNSUPPORTED,
}
