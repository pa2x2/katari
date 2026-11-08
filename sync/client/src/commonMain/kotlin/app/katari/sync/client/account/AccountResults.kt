package app.katari.sync.client.account

import app.katari.sync.client.server.Refusal
import app.katari.sync.protocol.device.DeviceId
import app.katari.sync.protocol.device.LinkedDevice
import app.katari.sync.protocol.version.ProtocolCompatibility
import okhttp3.HttpUrl

/** What checking a server address found, before anyone signs in (D027). */
sealed interface ServerCheck {
    /**
     * A sync server this app can sync with; on a local network over plain HTTP when [isPlainHttp], to warn about.
     * [instanceId] names its storage ([app.katari.sync.protocol.server.ServerInfo.instanceId]), which tells the server a
     * device syncs with at another address apart from another server.
     */
    data class Ready(val server: HttpUrl, val isPlainHttp: Boolean, val instanceId: String) : ServerCheck

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

/** How following this device's server to another address ended. */
sealed interface ServerMove {
    /** The device syncs with the server at its new address now, keeping its sign-in. */
    data object Moved : ServerMove

    /** The address didn't pass [ServerCheck] as [ServerCheck.Ready]. */
    data class NotReady(val check: ServerCheck) : ServerMove

    /** A sync server answers there, but not the one this device syncs with; joining it is signing in anew. */
    data object OtherServer : ServerMove
}

/** How signing in, redeeming a link code or redeeming an invite ended. */
sealed interface JoinResult {
    /**
     * The device belongs to the account now; its first sync runs in the background. [choicesWaiting] when it holds
     * data of its own and the account does too, so the person picks how its profiles join before it syncs (D027).
     */
    data class Joined(val choicesWaiting: Boolean) : JoinResult

    /** The address didn't pass [ServerCheck] as [ServerCheck.Ready]. */
    data class ServerNotReady(val check: ServerCheck) : JoinResult

    data class Refused(val reason: Refusal) : JoinResult

    data object Unreachable : JoinResult

    /** The platform has nowhere to keep the device's token safely, such as a desktop without a keyring. */
    data object NoSecretStore : JoinResult
}

/** What looking up a link code before joining with it found (D027). */
sealed interface LinkCodeLookup {
    /** [fromDevice] names the device that shows the code; null when the server doesn't say. */
    data class Found(val accountName: String, val fromDevice: String?) : LinkCodeLookup

    /** Why the code can't be joined with, as joining with it would have ended. */
    data class Failed(val result: JoinResult) : LinkCodeLookup
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
