package app.katari.sync.client.server

import app.katari.sync.protocol.error.SyncError
import app.katari.sync.protocol.error.SyncErrorCode
import app.katari.sync.protocol.version.ProtocolCompatibility
import app.katari.sync.protocol.version.SyncProtocolVersion
import java.io.IOException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** Why a call to the sync server didn't get the answer it asked for. */
sealed class SyncFailure(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /**
     * No answer: no network, a name that doesn't resolve, a refused or dropped connection, a timeout, a certificate
     * the device doesn't trust. Retried later; nothing about the connection changes.
     */
    class Unreachable(cause: IOException) : SyncFailure("Can't reach the sync server: ${cause.message}", cause)

    /** Something answered, but not as a Katari sync server does: a web page, a redirect, a proxy's error. */
    class NotASyncServer(val status: Int) : SyncFailure("Not a sync server's answer (HTTP $status)")

    /** The server refused the call for [reason]; [withToken] when the call carried the device's token. */
    class Refused(val error: SyncError, withToken: Boolean) : SyncFailure("Refused: ${error.code} ${error.message}") {
        val reason: Refusal = refusalOf(error).let {
            if (withToken && it == Refusal.NotAccepted) Refusal.TokenNotAccepted else it
        }
    }
}

/** What a refusal asks of the device. */
sealed interface Refusal {
    /**
     * The username and password, the link code or the invite didn't match, or the code was used or ran out; the
     * server doesn't say which, so a guess learns nothing. A refused token is [TokenNotAccepted] instead.
     */
    data object NotAccepted : Refusal

    /** Too many failed sign-ins from here or for the account; the next is checked after [retryAfter]. */
    data class Throttled(val retryAfter: Duration) : Refusal

    /**
     * An `UNAUTHORIZED` answer to a call that carried the device's token: the server doesn't know the token, so it
     * isn't the server the device registered with, or was restored from a copy older than the registration.
     */
    data object TokenNotAccepted : Refusal

    data object DeviceRemoved : Refusal

    data object AccountDisabled : Refusal

    data object UsernameTaken : Refusal

    /** The server holds nothing to check the device's position against any more; it resyncs, keeping deletions out. */
    data object PositionExpired : Refusal

    /** The server's history isn't the one the device's position came from: it was lost or restored. */
    data object PositionUnknown : Refusal

    /** The app's protocol version is outside the server's range; [compatibility] says which side must update. */
    data class Incompatible(val compatibility: ProtocolCompatibility) : Refusal
}

private fun refusalOf(error: SyncError): Refusal = when (error.code) {
    SyncErrorCode.UNAUTHORIZED -> if (error.retryAfterMillis > 0) {
        Refusal.Throttled(error.retryAfterMillis.milliseconds)
    } else {
        Refusal.NotAccepted
    }
    SyncErrorCode.DEVICE_REVOKED -> Refusal.DeviceRemoved
    SyncErrorCode.ACCOUNT_DISABLED -> Refusal.AccountDisabled
    SyncErrorCode.USERNAME_TAKEN -> Refusal.UsernameTaken
    SyncErrorCode.CURSOR_EXPIRED -> Refusal.PositionExpired
    SyncErrorCode.CURSOR_UNKNOWN -> Refusal.PositionUnknown
    // A server that sends no range refuses every version this app speaks, so it is the one behind.
    SyncErrorCode.PROTOCOL_VERSION_UNSUPPORTED -> Refusal.Incompatible(
        error.supportedVersions?.compatibilityOf(SyncProtocolVersion.CURRENT) ?: ProtocolCompatibility.SERVER_TOO_OLD,
    )
}
