package app.katari.sync.client

import app.katari.core.platform.network.NetworkConnectivity
import app.katari.core.platform.network.NetworkState
import app.katari.sync.client.account.DeviceDirectory
import app.katari.sync.client.account.DeviceListChanges
import app.katari.sync.client.account.Disconnection
import app.katari.sync.client.account.JoinResult
import app.katari.sync.client.account.SyncAccount
import app.katari.sync.client.capture.SyncCapture
import app.katari.sync.client.connection.DeviceNaming
import app.katari.sync.client.connection.DeviceTokenStore
import app.katari.sync.client.connection.SyncConnection
import app.katari.sync.client.connection.ThisDevice
import app.katari.sync.client.file.transfer.FilePlacement
import app.katari.sync.client.file.transfer.FileTransferRule
import app.katari.sync.client.file.transfer.FileUploads
import app.katari.sync.client.file.transfer.PendingFiles
import app.katari.sync.client.firstsync.ExistingFiles
import app.katari.sync.client.firstsync.ExistingRows
import app.katari.sync.client.firstsync.ExistingSettings
import app.katari.sync.client.firstsync.FirstSyncSteps
import app.katari.sync.client.handoff.EntryHandOff
import app.katari.sync.client.record.UngroupedRecords
import app.katari.sync.client.resync.FullResync
import app.katari.sync.client.run.PullChanges
import app.katari.sync.client.run.PushChanges
import app.katari.sync.client.run.ServerContinuity
import app.katari.sync.client.run.SyncOutcome
import app.katari.sync.client.run.SyncPass
import app.katari.sync.client.run.SyncRuns
import app.katari.sync.client.run.SyncSteps
import app.katari.sync.client.server.SyncServerClient
import app.katari.sync.client.server.SyncServerClients
import app.katari.sync.client.status.SyncStatusFlow
import app.katari.sync.client.trigger.LiveUpdates
import app.katari.sync.client.trigger.LocalChanges
import app.katari.sync.client.trigger.SyncBackgroundWork
import app.katari.sync.client.trigger.SyncTriggers
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import java.io.File

/**
 * A [SyncTestDevice] with sync's transport wired as the app wires it, talking to a real server over [http]: a test
 * passes a client of its own to cut calls short where a real network would.
 */
internal class SyncingTestDevice(file: File, http: OkHttpClient = OkHttpClient()) : AutoCloseable {
    val local = SyncTestDevice(file)
    private val tokens = MemoryTokens()
    val network = Network()
    val work = RecordedBackgroundWork()
    private val clients = SyncServerClients { http }
    val connection = SyncConnection(local.handler, tokens)
    private val capture = SyncCapture(local.handler, local.temporarySchema, local.settingsCapture, local.fileCapture)
    private val directory = DeviceDirectory()
    val handOff = EntryHandOff(local.handler, local.incoming, local.preferences.reads, directory)
    private val disconnection = Disconnection(capture, connection, handOff)
    private val resync = FullResync(local.handler, local.writing)
    private val firstSync = FirstSyncSteps(
        local.firstSync,
        connection,
        ExistingRows(local.driver, local.handler, local.drain),
        ExistingFiles(local.handler, local.ownedFiles, local.syncPreferences),
        ExistingSettings(local.handler, local.preferences.reads, local.drain),
        resync,
    )
    val preferences = local.syncPreferences
    private val continuity = ServerContinuity(local.handler, connection)
    private val fileRule = FileTransferRule(network, preferences)
    private val uploads = FileUploads(local.handler, local.ownedFiles, fileRule)
    val naming = DeviceNaming(
        object : ThisDevice {
            override fun platformName() = file.nameWithoutExtension

            override val platform = "Linux"

            override val appVersion get() = local.appVersion
        },
        preferences,
    )
    val steps = SyncSteps()
    val runs = SyncRuns(
        SyncPass(
            connection, clients, network, preferences, firstSync, resync,
            PullChanges(connection, continuity, local.incoming, handOff, naming),
            PushChanges(
                connection,
                continuity,
                local.pending,
                uploads,
            ),
            local.incoming, local.pending, local.settingsCapture,
            local.fileCapture, local.drain, uploads,
            FilePlacement(local.handler, local.ownedFiles, fileRule, local.files.directories, preferences),
            handOff,
            disconnection,
            steps,
            UngroupedRecords(local.handler),
        ),
        steps,
    )
    private val deviceChanges = DeviceListChanges()
    val account = SyncAccount(
        clients, connection, tokens, naming, capture, runs, resync, firstSync, directory, disconnection,
        deviceChanges,
    )
    val triggers = SyncTriggers(
        runs,
        LiveUpdates(connection, clients, network, runs, deviceChanges, handOff),
        LocalChanges(local.driver),
        work,
        connection,
        preferences,
        local.handler,
        local.preferences.profiles,
        handOff,
    )
    val status = SyncStatusFlow(
        connection,
        runs,
        network,
        local.pending,
        local.questions,
        local.held,
        PendingFiles(local.handler, preferences),
        fileRule,
        steps,
    )

    /** Joins [accountId] with a link code; joining starts a run of its own, which [sync] waits for or follows. */
    suspend fun join(server: TestSyncServer, accountId: Long) {
        account.redeemLinkCode(server.address, server.linkCode(accountId)).shouldBeInstanceOf<JoinResult.Joined>()
    }

    suspend fun sync(): SyncOutcome = runs.runAndWait()

    /** A client of the account this device is linked to, for a test to look at the server's side. */
    suspend fun client(): SyncServerClient =
        clients.of(checkNotNull(connection.state().server), checkNotNull(connection.token()))

    /**
     * What sync carries of this device's data: chapters neither read nor bookmarked come from the source on every
     * device, not from sync.
     */
    suspend fun library() = local.library().filterNot { it.startsWith("[\"chapter\"") && it.endsWith(",0,0]") }

    suspend fun sql(vararg statements: String) = local.sql(*statements)

    override fun close() {
        // Joining starts a run of its own, which a test may not wait for; closing the database under it crashes SQLite
        runBlocking { runs.running.first { !it } }
        local.close()
    }

    private class MemoryTokens : DeviceTokenStore {
        @Volatile
        private var token: String? = null

        override val isAvailable = true

        override fun read() = token

        override fun write(token: String?) {
            this.token = token?.takeIf(String::isNotBlank)
        }
    }

    /** On Wi-Fi until a test puts it on mobile data. */
    class Network : NetworkConnectivity {
        private val state = MutableStateFlow(NetworkState(hasNetwork = true, isOnline = true, isMetered = false, true))

        fun onWifi(wifi: Boolean) {
            state.value = state.value.copy(isMetered = !wifi, countsAsWifi = wifi)
        }

        override fun state() = state

        override fun current() = state.value

        override val knowsWifi = true
    }

    class RecordedBackgroundWork : SyncBackgroundWork {
        @Volatile
        var periodic = false
            private set

        override suspend fun schedulePeriodic(enabled: Boolean) {
            periodic = enabled
        }

        override fun handOff() = Unit

        override fun longRun(onMetered: Boolean) = Unit

        override fun dropLongRun() = Unit

        override fun allowsBackgroundSync() = true
    }
}
