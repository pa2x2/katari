package app.katari.sync.client.account

import app.katari.sync.client.server.Refusal
import app.katari.sync.protocol.device.DeviceId
import app.katari.sync.protocol.device.LinkedDevice
import app.katari.sync.protocol.version.ProtocolCompatibility
import okhttp3.HttpUrl

/** What checking a server address found, before anyone signs in (D027). */
sealed interface ServerCheck {
    /** A sync server this app can sync with; on a local network over plain HTTP when [isPlainHttp], to warn about. */
    data class Ready(val server: HttpUrl, val isPlainHttp: Boolean) : ServerCheck

    /** A sync server with no accounts yet: they are made on its web page at [webPage], with its setup code. */
    data class NeedsSetup(val webPage: HttpUrl) : ServerCheck

    /** A sync server whose protocol versions don't include this app's; [compatibility] says which side is behind. */
    data class Incompatible(val compatibility: ProtocolCompatibility) : ServerCheck

    data object Malformed : ServerCheck

    /** Plain `http://` to a host off the local network, which sign-in refuses. */
    data object PlainHttpOutsideLocalNetwork : ServerCheck

    data object Unreachable : ServerCheck

    /** Something answered at the address, but not a Katari sync server. */
    data object NotASyncServer : ServerCheck
}

/** How signing in, redeeming a link code or redeeming an invite ended. */
sealed interface JoinResult {
    /** The device belongs to the account now; its first sync runs in the background. */
    data object Joined : JoinResult

    /** The address didn't pass [ServerCheck] as [ServerCheck.Ready]. */
    data class ServerNotReady(val check: ServerCheck) : JoinResult

    data class Refused(val reason: Refusal) : JoinResult

    data object Unreachable : JoinResult

    /** The platform has nowhere to keep the device's token safely, such as a desktop without a keyring. */
    data object NoSecretStore : JoinResult
}

/** A device of the account as the Sync page lists it (D050). */
data class AccountDevice(val device: LinkedDevice, val isThisDevice: Boolean) {
    val id: DeviceId get() = device.id
}

/**
 * A one-time code a new device joins with, shown with the server's address, which the QR code of [link] carries too
 * (D024). [expiresAtMillis] is the server's time.
 */
data class LinkCodeOffer(val code: String, val expiresAtMillis: Long, val link: LinkCodeLink) {
    val server: HttpUrl get() = link.server
}

/** The answer to a call the Sync page makes to the server once the device is linked. */
sealed interface AccountCall<out T> {
    data class Done<T>(val value: T) : AccountCall<T>

    data object NotLinked : AccountCall<Nothing>

    data object Unreachable : AccountCall<Nothing>

    data class Refused(val reason: Refusal) : AccountCall<Nothing>
}
