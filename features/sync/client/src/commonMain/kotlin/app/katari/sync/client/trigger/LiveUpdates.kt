package app.katari.sync.client.trigger

import app.katari.core.platform.network.NetworkConnectivity
import app.katari.sync.client.account.DeviceListChanges
import app.katari.sync.client.connection.SyncConnection
import app.katari.sync.client.connection.SyncConnectionState
import app.katari.sync.client.connection.SyncStage
import app.katari.sync.client.handoff.EntryHandOff
import app.katari.sync.client.message.MessageInbox
import app.katari.sync.client.preference.SyncPreferences
import app.katari.sync.client.run.SyncRequest
import app.katari.sync.client.run.SyncRuns
import app.katari.sync.client.server.LiveChannel
import app.katari.sync.client.server.SyncFailure
import app.katari.sync.client.server.SyncServerClient
import app.katari.sync.client.server.SyncServerClients
import app.katari.sync.protocol.live.ChangesAvailable
import app.katari.sync.protocol.message.FetchMessagesRequest
import app.katari.sync.protocol.server.ServerIdentity
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * The live channel while the app is open (D021): an announcement of a position past this device's starts a run at once,
 * and announcements that keep coming at most every couple of seconds ([RunPacing]), so another device's change shows
 * here in about the time a run takes; its count of the account's devices goes to [DeviceListChanges]. A channel that
 * ends is opened again after a backoff that grows to [MAX_BACKOFF] and starts over once the channel works; each end
 * also starts a run, which finds out why when the server refused it, as for a removed device. A channel heard from
 * after the server or the network was found missing starts a run too, even with nothing new to pull: only a run that
 * gets through tells the status, and an open entry's note that its progress is this device's own, that the server
 * answers again. The server's first announcement on a new channel carries its current position, so a channel opened
 * again after the app was closed catches up on what came meanwhile without a run of its own.
 *
 * An announcement of a message from another device goes to the [MessageInbox] without a run. While the channel isn't
 * working, a fetch that the server holds open until a message comes takes its place for messages.
 */
@Inject
@SingleIn(AppScope::class)
class LiveUpdates(
    private val connection: SyncConnection,
    private val clients: SyncServerClients,
    private val network: NetworkConnectivity,
    private val runs: SyncRuns,
    private val deviceChanges: DeviceListChanges,
    private val handOff: EntryHandOff,
    private val preferences: SyncPreferences,
    private val inbox: MessageInbox,
) {
    private val state = MutableStateFlow(LiveUpdatesState.CLOSED)

    /** How the channel stands now, which the status shows. */
    val states: StateFlow<LiveUpdatesState> = state.asStateFlow()

    /**
     * Whether the channel should be open: the app is open ([appOpen]), the device syncs and isn't paused, and there is a
     * network that changes may use.
     */
    fun wanted(appOpen: Flow<Boolean>): Flow<Boolean> = combine(
        appOpen,
        connection.states(),
        network.state(),
        preferences.paused.changes(),
        preferences.changesOnMobileData.changes(),
    ) { isAppOpen, state, network, paused, onMobileData ->
        isAppOpen && state.listens() && network.hasNetwork && !paused && (onMobileData || network.countsAsWifi)
    }
        .distinctUntilChanged()

    /** Keeps the channel open until cancelled. */
    suspend fun listen(): Unit = coroutineScope {
        // Announcements arrive on OkHttp's thread; the position they are compared with is the newest one.
        val latest = AtomicReference(connection.state())
        val following = launch { connection.states().collect(latest::set) }
        val pacing = RunPacing()
        val announced = AtomicReference<ServerIdentity?>(null)
        val messages = Channel<ChangesAvailable>(Channel.CONFLATED)
        val receiving = launch {
            for (changes in messages) withClient(latest) { inbox.announced(changes, it) }
        }
        val polling = launch {
            state.collectLatest { if (it == LiveUpdatesState.NOT_WORKING) pollMessages(latest) }
        }
        val pulling = launch {
            pacing.follow {
                // The announcement of this device's own push comes while that push's run is going, before its answer
                // moves the device past it; once the run is over it has, and a pull would find nothing
                runs.running.first { !it }
                if (latest.get().isBehind(announced.get())) runs.request(SyncRequest())
            }
        }
        try {
            keepOpen(latest, pacing, announced, messages)
        } finally {
            following.cancel()
            receiving.cancel()
            polling.cancel()
            pulling.cancel()
            state.value = LiveUpdatesState.CLOSED
        }
    }

    private suspend fun keepOpen(
        latest: AtomicReference<SyncConnectionState>,
        pacing: RunPacing,
        announced: AtomicReference<ServerIdentity?>,
        messages: SendChannel<ChangesAvailable>,
    ) {
        var backoff = FIRST_BACKOFF
        var unheard = 0
        while (true) {
            val server = latest.get().server
            val token = connection.token()
            if (server == null || token == null) return
            val ended = CompletableDeferred<Unit>()
            val heard = AtomicBoolean(false)
            val channel = clients.of(server, token).live(
                object : LiveChannel.Listener {
                    override fun onAnnouncement(changes: ChangesAvailable) {
                        heard.set(true)
                        state.value = LiveUpdatesState.OPEN
                        deviceChanges.hear(changes.devices)
                        val state = latest.get()
                        if (changes.newestMessage > state.messagesHandled) messages.trySend(changes)
                        if (state.isBehind(changes.server)) {
                            announced.set(changes.server)
                            pacing.hear()
                        } else if (state.unreachableSince != null || handOff.toldUnreachable) {
                            runs.request(SyncRequest())
                        }
                    }

                    override fun onEnded(refused: Boolean) {
                        ended.complete(Unit)
                    }
                },
            )
            try {
                ended.await()
            } finally {
                channel.close()
            }
            runs.request(SyncRequest())
            if (heard.get()) {
                backoff = FIRST_BACKOFF
                unheard = 0
                state.value = LiveUpdatesState.CLOSED
            } else if (++unheard >= UNHEARD_TO_FAIL) {
                state.value = LiveUpdatesState.NOT_WORKING
            }
            delay(backoff)
            backoff = (backoff * 2).coerceAtMost(MAX_BACKOFF)
        }
    }

    // A channel that doesn't work can be a server that is down, so failing fetches back off as reopening the channel does
    private suspend fun pollMessages(latest: AtomicReference<SyncConnectionState>): Nothing {
        var backoff = FIRST_BACKOFF
        while (true) {
            if (withClient(latest) { inbox.fetch(it, wait = MESSAGE_WAIT) }) {
                backoff = FIRST_BACKOFF
            } else {
                delay(backoff)
                backoff = (backoff * 2).coerceAtMost(MAX_BACKOFF)
            }
        }
    }

    /**
     * Calls [block] with a client of the device's server; false when there is none or it failed. A refusal starts a run,
     * which finds out why, as for a removed device.
     */
    private suspend fun withClient(
        latest: AtomicReference<SyncConnectionState>,
        block: suspend (SyncServerClient) -> Unit,
    ): Boolean {
        val server = latest.get().server
        val token = connection.token()
        if (server == null || token == null) return false
        return try {
            block(clients.of(server, token))
            true
        } catch (e: SyncFailure.Refused) {
            logcat(LogPriority.INFO, e) { "Messages refused" }
            runs.request(SyncRequest())
            false
        } catch (e: SyncFailure) {
            logcat(LogPriority.INFO, e) { "Messages not fetched" }
            false
        }
    }

    private fun SyncConnectionState.isBehind(server: ServerIdentity?) =
        server != null && (server.sequence > position || server.instanceId != instanceId)

    private fun SyncConnectionState.listens() = isLinked && stopped == null && stage == SyncStage.READY

    private companion object {
        val FIRST_BACKOFF: Duration = 2.seconds
        val MAX_BACKOFF: Duration = 5.minutes
        val MESSAGE_WAIT: Duration = FetchMessagesRequest.MAX_WAIT_MILLIS.milliseconds

        // One channel that ends before the server says anything can be a restart; a second in a row is a pattern
        const val UNHEARD_TO_FAIL = 2
    }
}

enum class LiveUpdatesState {
    /** Not open, or opening: the app is closed, the device doesn't sync now, or the channel is being opened again. */
    CLOSED,

    /** Open, and the server has announced its position on it. */
    OPEN,

    /**
     * Each of the last channels ended before the server announced anything, as when a proxy in front of the server
     * doesn't pass WebSockets on, so changes from other devices come only with runs. Kept until a channel works.
     */
    NOT_WORKING,
}
