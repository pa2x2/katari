package app.katari.sync.client.account

import app.katari.sync.client.capture.SyncCapture
import app.katari.sync.client.connection.DeviceNaming
import app.katari.sync.client.connection.DeviceTokenStore
import app.katari.sync.client.connection.SyncConnection
import app.katari.sync.client.connection.SyncStage
import app.katari.sync.client.connection.SyncStop
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
    private val naming: DeviceNaming,
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

    /**
     * What [address] leads to. A local-network address typed without a scheme that finds no sync server over HTTPS is
     * tried over plain HTTP (D002); when nothing answers there, the HTTPS answer says why.
     */
    suspend fun checkServer(address: String): ServerCheck {
        val parsed = when (val parsed = ServerAddress.parse(address)) {
            is ServerAddress.Accepted -> parsed
            ServerAddress.Malformed -> return ServerCheck.Malformed
            ServerAddress.PlainHttpOutsideLocalNetwork -> return ServerCheck.PlainHttpOutsideLocalNetwork
        }
        val checked = checkServer(parsed.url)
        val fallback = parsed.plainHttpFallback
        if (fallback == null || (checked != ServerCheck.Unreachable && checked != ServerCheck.NotASyncServer)) {
            return checked
        }
        return checkServer(fallback).takeIf { it != ServerCheck.Unreachable } ?: checked
    }

    private suspend fun checkServer(url: HttpUrl): ServerCheck {
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
            val found = clients.of(check.server).accountOfLinkCode(code.trim())
            LinkCodeLookup.Found(found.accountName, found.fromDevice)
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
            register(clients.of(check.server), naming.description())
        } catch (e: SyncFailure.Refused) {
            return JoinResult.Refused(e.reason)
        } catch (_: SyncFailure.Unreachable) {
            return JoinResult.Unreachable
        } catch (_: SyncFailure.NotASyncServer) {
            return JoinResult.ServerNotReady(ServerCheck.NotASyncServer)
        }
        val choicesWaiting = runs.exclusive {
            start(check.server, registered)
            val client = clients.of(check.server, registered.token)
            setNameApart(client, registered.device)
            compareWithAccount(client)
        }
        runs.request()
        return JoinResult.Joined(choicesWaiting)
    }

    /**
     * Numbers this device's name when another device of the account has it (D005), before the first pull tells the
     * account. A server that can't be reached for the list leaves the name as it is.
     */
    private suspend fun setNameApart(client: SyncServerClient, device: DeviceId) {
        val listed = try {
            client.devices().devices
        } catch (_: SyncFailure) {
            return
        }
        devices.remember(listed)
        naming.setApart(listed.filter { it.id != device }.map { it.description.name })
    }

    /**
     * Takes the first step of the first sync now rather than in the run, so whoever signed in learns at once whether
     * the choices wait for them. A server that can't be reached for it leaves the step to the run.
     */
    private suspend fun compareWithAccount(client: SyncServerClient): Boolean {
        if (connection.state().stage != SyncStage.JOINING) return false
        try {
            firstSync.join(client)
        } catch (_: SyncFailure) {
            return false
        }
        return connection.state().stage == SyncStage.CHOOSING
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
        // The same account keeps what this device synced with it, and capture goes on with the edits made meanwhile
        // (D066). Whatever a connection to another account left meant something only there.
        val sameAccount = previous.enabled && previous.accountName == registered.accountName
        if (previous.enabled && !sameAccount) disconnection.leave(reason = null)
        connection.connect(server, registered, SyncStage.JOINING)
        if (!sameAccount) capture.start()
    }

    /** This device's ID in its account, known without the server; null while the device has never joined one. */
    suspend fun thisDevice(): DeviceId? = connection.state().device

    suspend fun devices(): AccountCall<List<AccountDevice>> = linked { client, device ->
        client.devices().devices.also(devices::remember).map { AccountDevice(it, isThisDevice = it.id == device) }
    }

    suspend fun createLinkCode(): AccountCall<LinkCodeOffer> = linked { client, _ ->
        val code = client.createLinkCode()
        val link = LinkCodeLink(client.server, code.code, connection.state().accountName, naming.name())
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
