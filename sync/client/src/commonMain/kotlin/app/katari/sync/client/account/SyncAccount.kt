package app.katari.sync.client.account

import app.katari.sync.client.capture.SyncCapture
import app.katari.sync.client.connection.DeviceTokenStore
import app.katari.sync.client.connection.SyncConnection
import app.katari.sync.client.connection.SyncStage
import app.katari.sync.client.connection.SyncStop
import app.katari.sync.client.connection.ThisDevice
import app.katari.sync.client.firstsync.FirstSyncSteps
import app.katari.sync.client.firstsync.ProfileChoice
import app.katari.sync.client.firstsync.ProfilePairing
import app.katari.sync.client.resync.FullResync
import app.katari.sync.client.run.SyncRequest
import app.katari.sync.client.run.SyncRuns
import app.katari.sync.client.server.ServerAddress
import app.katari.sync.client.server.SyncFailure
import app.katari.sync.client.server.SyncServerClient
import app.katari.sync.client.server.SyncServerClients
import app.katari.sync.protocol.device.DeviceDescription
import app.katari.sync.protocol.device.DeviceId
import app.katari.sync.protocol.device.RegisterDeviceResponse
import app.katari.sync.protocol.version.ProtocolCompatibility
import app.katari.sync.protocol.version.SyncProtocolVersion
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeoutOrNull
import logcat.LogPriority
import okhttp3.HttpUrl
import tachiyomi.core.common.util.system.logcat
import kotlin.time.Duration.Companion.seconds

/**
 * What the Sync screens ask of sync (D027, D030, D050): checking a server, joining an account by password, link
 * code or invite, the account's devices and link codes, the first-sync choices, and leaving.
 *
 * Joining starts capture at once and leaves the first sync to a background run, so the app is usable meanwhile. A
 * device that had to sign in again to the same server ([SyncStop.SIGN_IN_AGAIN]) keeps what it captured and resyncs
 * in full instead (D048): it asks nothing, since its records already merged with the account's before.
 */
@Inject
class SyncAccount(
    private val clients: SyncServerClients,
    private val connection: SyncConnection,
    private val tokens: DeviceTokenStore,
    private val thisDevice: ThisDevice,
    private val capture: SyncCapture,
    private val runs: SyncRuns,
    private val resync: FullResync,
    private val firstSync: FirstSyncSteps,
    private val devices: DeviceDirectory,
    private val disconnection: Disconnection,
    private val deviceChanges: DeviceListChanges,
) {
    /** Changes whenever the server says a device joined or left the account, while the app listens live. */
    val deviceListChanges: Flow<Long> get() = deviceChanges.heard

    suspend fun checkServer(address: String): ServerCheck {
        val url = when (val parsed = ServerAddress.parse(address)) {
            is ServerAddress.Accepted -> parsed.url
            ServerAddress.Malformed -> return ServerCheck.Malformed
            ServerAddress.PlainHttpOutsideLocalNetwork -> return ServerCheck.PlainHttpOutsideLocalNetwork
        }
        val info = try {
            clients.of(url).info()
        } catch (_: SyncFailure.Unreachable) {
            return ServerCheck.Unreachable
        } catch (_: SyncFailure) {
            return ServerCheck.NotASyncServer
        }
        val compatibility = info.protocolVersions.compatibilityOf(SyncProtocolVersion.CURRENT)
        return when {
            compatibility != ProtocolCompatibility.SUPPORTED -> ServerCheck.Incompatible(compatibility)
            info.needsSetup -> ServerCheck.NeedsSetup(url)
            else -> ServerCheck.Ready(url, isPlainHttp = !url.isHttps)
        }
    }

    suspend fun signIn(address: String, username: String, password: String): JoinResult =
        join(address) { client, device -> client.signIn(device, username.trim(), password) }

    /**
     * The account a link code joins, for asking before joining with it (D027); the code stays usable. Fails as joining
     * with the code would.
     */
    suspend fun lookUpLinkCode(address: String, code: String): LinkCodeLookup {
        val check = checkServer(address)
        if (check !is ServerCheck.Ready) return LinkCodeLookup.Failed(JoinResult.ServerNotReady(check))
        return try {
            LinkCodeLookup.Found(clients.of(check.server).accountOfLinkCode(code.trim()).accountName)
        } catch (e: SyncFailure.Refused) {
            LinkCodeLookup.Failed(JoinResult.Refused(e.reason))
        } catch (_: SyncFailure.Unreachable) {
            LinkCodeLookup.Failed(JoinResult.Unreachable)
        } catch (_: SyncFailure.NotASyncServer) {
            LinkCodeLookup.Failed(JoinResult.ServerNotReady(ServerCheck.NotASyncServer))
        }
    }

    suspend fun redeemLinkCode(address: String, code: String): JoinResult =
        join(address) { client, device -> client.redeemLinkCode(device, code.trim()) }

    suspend fun redeemInvite(address: String, code: String, username: String, password: String): JoinResult =
        join(address) { client, device -> client.redeemInvite(device, code.trim(), username.trim(), password) }

    private suspend fun join(
        address: String,
        register: suspend (SyncServerClient, DeviceDescription) -> RegisterDeviceResponse,
    ): JoinResult {
        if (!tokens.isAvailable) return JoinResult.NoSecretStore
        val check = checkServer(address)
        if (check !is ServerCheck.Ready) return JoinResult.ServerNotReady(check)
        val registered = try {
            register(clients.of(check.server), thisDevice.description())
        } catch (e: SyncFailure.Refused) {
            return JoinResult.Refused(e.reason)
        } catch (_: SyncFailure.Unreachable) {
            return JoinResult.Unreachable
        } catch (_: SyncFailure.NotASyncServer) {
            return JoinResult.ServerNotReady(ServerCheck.NotASyncServer)
        }
        runs.exclusive { start(check.server, registered) }
        runs.request()
        return JoinResult.Joined
    }

    private suspend fun start(server: HttpUrl, registered: RegisterDeviceResponse) {
        val previous = connection.state()
        // Signing in again to the account and server storage the device synced with resumes it in full. Another
        // account, or a server that lost its storage and started over, is one the device never synced with: it joins
        // as a device with data of its own, so its profiles pair with the account's by name (D065).
        val returning = previous.enabled && previous.stopped == SyncStop.SIGN_IN_AGAIN && previous.server == server &&
            previous.instanceId == registered.server.instanceId && previous.accountName == registered.accountName
        if (returning) {
            connection.connect(server, registered, SyncStage.RESYNC)
            resync.start(SyncStage.RESYNC, registered.server)
            return
        }
        // Whatever an earlier connection left meant something only on its own server.
        if (previous.enabled) disconnection.leave(reason = null)
        connection.connect(server, registered, SyncStage.JOINING)
        capture.start()
    }

    suspend fun devices(): AccountCall<List<AccountDevice>> = linked { client, device ->
        client.devices().devices.also(devices::remember).map { AccountDevice(it, isThisDevice = it.id == device) }
    }

    suspend fun createLinkCode(): AccountCall<LinkCodeOffer> = linked { client, _ ->
        val code = client.createLinkCode()
        val link = LinkCodeLink(client.server, code.code, connection.state().accountName, thisDevice.description().name)
        LinkCodeOffer(code.code, code.expiresAtMillis, link)
    }

    /** Removes another device of the account; this device leaves with [disconnect]. */
    suspend fun removeDevice(id: DeviceId): AccountCall<Boolean> = linked { client, device ->
        require(id != device) { "This device leaves by disconnecting" }
        client.revoke(id)
    }

    /** This device's profiles paired with the account's, for a device waiting at [SyncStage.CHOOSING]. */
    suspend fun firstSyncProposal(): AccountCall<List<ProfilePairing>> = linked { client, _ ->
        firstSync.proposal(client)
    }

    suspend fun answerFirstSync(choices: List<ProfileChoice>) {
        runs.exclusive { firstSync.choose(choices) }
        runs.request()
    }

    /**
     * Leaves the account (D030): what is waiting to be sent goes first, the server forgets this device when it can be
     * reached, and the device keeps all its data, standalone again.
     */
    suspend fun disconnect() {
        if (!connection.state().isLinked) return
        withTimeoutOrNull(LAST_PUSH_TIMEOUT) { runs.runAndWait(SyncRequest(onlyIfChanged = true)) }
        runs.exclusive {
            val state = connection.state()
            val server = state.server
            val device = state.device
            val token = connection.token()
            if (server != null && device != null && token != null) {
                try {
                    withTimeoutOrNull(LAST_PUSH_TIMEOUT) { clients.of(server, token).revoke(device) }
                } catch (e: SyncFailure) {
                    // It stays in the device list until removed there; its token works for nobody but this device.
                    logcat(LogPriority.INFO, e) { "Couldn't tell the sync server this device left" }
                }
            }
            disconnection.leave(reason = null)
        }
    }

    suspend fun syncNow() {
        runs.request()
    }

    /** Forgets why the device stopped syncing once the person has read it. */
    suspend fun dismissNotice() {
        connection.dismissNotice()
    }

    private suspend fun <T> linked(call: suspend (SyncServerClient, DeviceId) -> T): AccountCall<T> {
        val state = connection.state()
        val server = state.server
        val device = state.device
        val token = connection.token()
        if (!state.isLinked || server == null || device == null || token == null) return AccountCall.NotLinked
        return try {
            AccountCall.Done(call(clients.of(server, token), device))
        } catch (e: SyncFailure.Refused) {
            // A run acts on what the refusal says about the connection, such as this device having been removed.
            runs.request()
            AccountCall.Refused(e.reason)
        } catch (_: SyncFailure) {
            AccountCall.Unreachable
        }
    }

    private companion object {
        val LAST_PUSH_TIMEOUT = 15.seconds
    }
}
