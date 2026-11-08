package app.katari.sync.client.connection

import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.katari.sync.protocol.device.RegisterDeviceResponse
import app.katari.sync.protocol.server.ServerIdentity
import app.katari.sync.protocol.version.SyncProtocolVersion
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import logcat.LogPriority
import okhttp3.HttpUrl
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.DatabaseHandler
import kotlin.time.Clock

/**
 * This device's connection to its account: the state in `sync_state`, which no backup holds, and the token in the
 * platform's secret store. The token is remembered once read, since every call to the server needs it.
 */
@Inject
@SingleIn(AppScope::class)
class SyncConnection(
    private val handler: DatabaseHandler,
    private val tokens: DeviceTokenStore,
) {
    @Volatile
    private var token: Remembered? = null

    suspend fun state(): SyncConnectionState = handler.await {
        SyncConnectionState.of(sync_stateQueries.get().awaitAsOne())
    }

    fun states(): Flow<SyncConnectionState> = handler.subscribeToOne { sync_stateQueries.get() }
        .map(SyncConnectionState::of)
        .distinctUntilChanged()

    suspend fun token(): String? {
        token?.let { return it.value }
        return withContext(Dispatchers.IO) { tokens.read() }.also { token = Remembered(it) }
    }

    /**
     * Keeps what registering with [server] gave the device, starting the connection at [stage]. The token is saved
     * first: a connection the device can't sign its calls for is no connection.
     */
    suspend fun connect(server: HttpUrl, registered: RegisterDeviceResponse, stage: SyncStage) {
        withContext(Dispatchers.IO) { tokens.write(registered.token) }
        token = Remembered(registered.token)
        handler.await {
            sync_stateQueries.connect(
                serverUrl = server.toString(),
                deviceId = registered.device.value,
                accountName = registered.accountName,
                serverInstanceId = registered.server.instanceId,
                stage = stage.stored,
            )
        }
    }

    /** Keeps [server] as where this device's server is now, the same one at another address. */
    suspend fun moveTo(server: HttpUrl) {
        handler.await(inTransaction = true) {
            val device = sync_stateQueries.get().awaitAsOne().device_id
            sync_stateQueries.setServer(serverUrl = server.toString(), deviceId = device)
        }
    }

    suspend fun setStage(stage: SyncStage, target: Long = 0) {
        handler.await { sync_stateQueries.setStage(stage.stored, target) }
    }

    /**
     * Moves the device's position to [server]'s after a push the server says was all it took since, so the device
     * doesn't pull its own records back (D064).
     */
    suspend fun passOwnPush(server: ServerIdentity) {
        handler.await {
            sync_stateQueries.passOwnPush(
                cursor = server.sequence,
                chain = server.chain,
                serverInstanceId = server.instanceId,
            )
        }
    }

    /** Starts a full resync at [stage] from [server]'s first record. */
    suspend fun restart(stage: SyncStage, server: ServerIdentity) {
        handler.await { sync_stateQueries.restartPosition(stage.stored, server.sequence, server.instanceId) }
    }

    suspend fun stop(reason: SyncStop?) {
        val protocol = if (reason == SyncStop.APP_TOO_OLD) SyncProtocolVersion.CURRENT.toLong() else 0
        handler.await { sync_stateQueries.setStopped(reason?.stored, protocol) }
    }

    /** The server answered; [synced] when a whole sync finished with it. */
    suspend fun reached(synced: Boolean) {
        val now = Clock.System.now().toEpochMilliseconds()
        handler.await { sync_stateQueries.reached(now.takeIf { synced }) }
    }

    suspend fun unreachable() {
        handler.await { sync_stateQueries.unreachable(Clock.System.now().toEpochMilliseconds()) }
    }

    /** Forgets the token and the connection, leaving [reason] for the person to read; capture is stopped apart. */
    suspend fun forget(reason: SyncStop?) {
        forgetToken()
        handler.await(inTransaction = true) {
            sync_stateQueries.forget(reason?.stored)
            sync_recordsQueries.clear()
            sync_pendingQueries.clear()
            sync_outboxQueries.clear()
            sync_filesQueries.clear()
            sync_filesQueries.clearUploads()
        }
    }

    /** Forgets the token alone, keeping the device's records for when it signs in again. */
    suspend fun forgetToken() {
        token = Remembered(null)
        try {
            withContext(Dispatchers.IO) { tokens.write(null) }
        } catch (e: Exception) {
            // The server no longer takes it either way; a keyring that can't be reached keeps a dead token.
            logcat(LogPriority.WARN, e) { "Can't forget the sync device token" }
        }
    }

    suspend fun dismissNotice() {
        handler.await { sync_stateQueries.dismiss() }
    }

    private class Remembered(val value: String?)
}
