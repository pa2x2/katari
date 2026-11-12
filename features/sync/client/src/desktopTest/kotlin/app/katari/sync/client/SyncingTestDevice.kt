package app.katari.sync.client

import app.katari.core.platform.network.NetworkConnectivity
import app.katari.core.platform.network.NetworkState
import app.katari.sync.client.account.DeviceDirectory
import app.katari.sync.client.account.DeviceListChanges
import app.katari.sync.client.account.DeviceListFollowing
import app.katari.sync.client.account.Disconnection
import app.katari.sync.client.account.JoinResult
import app.katari.sync.client.account.SyncAccount
import app.katari.sync.client.capture.SyncCapture
import app.katari.sync.client.connection.DeviceNaming
import app.katari.sync.client.connection.DeviceTokenStore
import app.katari.sync.client.connection.SyncConnection
import app.katari.sync.client.connection.ThisDevice
import app.katari.sync.client.downloadrequest.DownloadRequestInbox
import app.katari.sync.client.downloadrequest.DownloadRequestResult
import app.katari.sync.client.downloadrequest.DownloadRequestRetry
import app.katari.sync.client.downloadrequest.DownloadRequestSending
import app.katari.sync.client.downloadrequest.ReceivedDownloadRequest
import app.katari.sync.client.extension.OtherDeviceExtensions
import app.katari.sync.client.file.transfer.FilePlacement
import app.katari.sync.client.file.transfer.FileTransferRule
import app.katari.sync.client.file.transfer.FileUploads
import app.katari.sync.client.file.transfer.PendingFiles
import app.katari.sync.client.firstsync.ExistingFiles
import app.katari.sync.client.firstsync.ExistingRows
import app.katari.sync.client.firstsync.ExistingSettings
import app.katari.sync.client.firstsync.FirstSyncSteps
import app.katari.sync.client.handoff.EntryHandOff
import app.katari.sync.client.handoff.LastReadDevices
import app.katari.sync.client.identity.SyncedEntries
import app.katari.sync.client.libraryupdate.OtherDeviceLibraryUpdates
import app.katari.sync.client.message.DeviceMessaging
import app.katari.sync.client.message.MessageHandler
import app.katari.sync.client.message.MessageHandlers
import app.katari.sync.client.message.MessageInbox
import app.katari.sync.client.message.MessageOutbox
import app.katari.sync.client.part.SyncChoiceChanges
import app.katari.sync.client.record.ChangeTransferRule
import app.katari.sync.client.record.UngroupedRecords
import app.katari.sync.client.restore.RestoreStamping
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
import app.katari.sync.protocol.device.DeviceId
import app.katari.sync.protocol.message.DeviceMessage
import app.katari.sync.protocol.message.MessageBody
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import java.io.File
import java.util.Collections
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

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
    val directory = DeviceDirectory(local.handler)
    val handOff = EntryHandOff(local.handler, local.incoming, local.preferences.reads, directory)
    private val disconnection = Disconnection(capture, connection, handOff, directory)
    val lastReadDevices = LastReadDevices(local.handler, connection, directory)
    val otherDeviceLibraryUpdates = OtherDeviceLibraryUpdates(local.handler, connection, directory)
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
    val otherDeviceExtensions = OtherDeviceExtensions(local.handler, connection, directory, preferences)
    private val continuity = ServerContinuity(local.handler, connection)
    private val fileRule = FileTransferRule(network, preferences)
    private val changeRule = ChangeTransferRule(network, preferences)
    private val uploads = FileUploads(local.handler, local.ownedFiles, fileRule)

    /** The text of each note another device sent here, as its handler took it. */
    val notes: MutableList<String> = Collections.synchronizedList(mutableListOf())

    /** The download requests other devices sent here, as this device carried them out once their entry was here. */
    val downloadRequests: MutableList<ReceivedDownloadRequest> = Collections.synchronizedList(mutableListOf())
    val syncedEntries = SyncedEntries(local.handler)
    val downloadRequestInbox = DownloadRequestInbox(local.handler, connection, syncedEntries, directory) { request ->
        downloadRequests += request
        DownloadRequestResult.DONE
    }
    private val messageHandlers = MessageHandlers(
        lazyOf(
            setOf(
                downloadRequestInbox,
                object : MessageHandler {
                    override val kind = NOTE

                    override val version = 1

                    override suspend fun handle(message: DeviceMessage) {
                        notes += message.body.payload.decodeToString()
                    }
                },
            ),
        ),
    )
    val naming = DeviceNaming(
        object : ThisDevice {
            override fun platformName() = file.nameWithoutExtension

            override val platform = "Linux"

            override val appVersion get() = local.appVersion
        },
        preferences,
        messageHandlers,
    )
    val steps = SyncSteps()
    private val messageOutbox = MessageOutbox(local.handler)

    private val messageInbox = MessageInbox(connection, messageHandlers)
    val runs = SyncRuns(
        SyncPass(
            connection, clients, network, preferences, firstSync, resync,
            PullChanges(connection, continuity, local.incoming, handOff, naming, directory),
            PushChanges(
                connection,
                continuity,
                local.pending,
                uploads,
                local.incoming,
            ),
            local.incoming, local.pending, local.settingsCapture,
            local.fileCapture, local.drain, uploads,
            FilePlacement(local.handler, local.ownedFiles, fileRule, local.files.directories, preferences),
            handOff,
            disconnection,
            steps,
            UngroupedRecords(local.handler),
            messageOutbox,
            messageInbox,
            changeRule,
            lazyOf(setOf(DownloadRequestRetry(downloadRequestInbox))),
        ),
        steps,
    )
    val restoreStamping =
        RestoreStamping(local.handler, local.writing, local.settingsCapture, local.preferences.reads, runs)
    val messaging = DeviceMessaging(connection, messageOutbox, runs)
    val downloadRequestSending = DownloadRequestSending(messaging, syncedEntries)
    val choiceChanges = SyncChoiceChanges(
        local.handler,
        preferences,
        local.writing,
        local.preferences.reads,
        connection,
        runs,
        local.fileCapture,
    )
    private val deviceChanges = DeviceListChanges()
    val account = SyncAccount(
        clients, connection, tokens, naming, capture, runs, resync, firstSync, directory, disconnection,
        deviceChanges,
    )
    private val live = LiveUpdates(
        connection,
        clients,
        network,
        runs,
        deviceChanges,
        handOff,
        preferences,
        messageInbox,
        changeRule,
    )
    val triggers = SyncTriggers(
        runs,
        live,
        LocalChanges(local.driver),
        work,
        connection,
        preferences,
        local.handler,
        local.preferences.profiles,
        handOff,
        changeRule,
        DeviceListFollowing(deviceChanges, account),
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
        preferences,
        live,
    )

    /** Joins [accountId] with a link code; joining starts a run of its own, which [sync] waits for or follows. */
    suspend fun join(server: TestSyncServer, accountId: Long) {
        account.redeemLinkCode(server.address, server.linkCode(accountId)).shouldBeInstanceOf<JoinResult.Joined>()
    }

    suspend fun sync(): SyncOutcome = runs.runAndWait()

    suspend fun sendNote(text: String, to: SyncingTestDevice) {
        val body = MessageBody(kind = NOTE, kindVersion = 1, payload = text.encodeToByteArray())
        messaging.send(to.connection.state().device, body, lifetime = 1.hours)
    }

    /** A client of the account this device is linked to, for a test to look at the server's side. */
    suspend fun client(): SyncServerClient =
        clients.of(checkNotNull(connection.state().server), checkNotNull(connection.token()))

    /**
     * What sync carries of this device's data: chapters neither read nor bookmarked come from the source on every
     * device, not from sync.
     */
    suspend fun library() = local.library().filterNot { it.startsWith("[\"chapter\"") && it.endsWith(",0,0]") }

    suspend fun sql(vararg statements: String) = local.sql(*statements)

    suspend fun entryId(url: String): Long = local.entryId(url)

    suspend fun deviceId(): DeviceId = checkNotNull(connection.state().device)

    override fun close() {
        // Joining and changing a sync choice start runs a test may not wait for, the triggers a test started go on by
        // themselves, and a run that synced has download requests looked at after it; closing the database under any
        // of them fails
        runBlocking {
            triggers.stop()
            runs.awaitIdle()
            downloadRequestInbox.awaitIdle()
        }
        local.close()
    }

    companion object {
        /** The only kind of message a test device takes. */
        const val NOTE = "note"
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
    }

    class RecordedBackgroundWork : SyncBackgroundWork {
        @Volatile
        var periodic = false
            private set

        override fun keepConnected(connected: Boolean) = Unit

        override suspend fun schedulePeriodic(enabled: Boolean, every: Duration) {
            periodic = enabled
        }

        override fun handOff() = Unit

        override fun longRun(onMetered: Boolean) = Unit

        override fun dropLongRun() = Unit

        override fun allowsBackgroundSync() = true
    }
}
