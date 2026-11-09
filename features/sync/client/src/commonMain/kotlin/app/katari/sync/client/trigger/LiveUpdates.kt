package app.katari.sync.client.trigger

import app.katari.core.platform.network.NetworkConnectivity
import app.katari.sync.client.account.DeviceListChanges
import app.katari.sync.client.connection.SyncConnection
import app.katari.sync.client.connection.SyncConnectionState
import app.katari.sync.client.connection.SyncStage
import app.katari.sync.client.handoff.EntryHandOff
import app.katari.sync.client.preference.SyncPreferences
import app.katari.sync.client.run.SyncRequest
import app.katari.sync.client.run.SyncRuns
import app.katari.sync.client.server.LiveChannel
import app.katari.sync.client.server.SyncServerClients
import app.katari.sync.protocol.live.ChangesAvailable
import app.katari.sync.protocol.server.ServerIdentity
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration
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
 */
@Inject
class LiveUpdates(
    private val connection: SyncConnection,
    private val clients: SyncServerClients,
    private val network: NetworkConnectivity,
    private val runs: SyncRuns,
    private val deviceChanges: DeviceListChanges,
    private val handOff: EntryHandOff,
    private val preferences: SyncPreferences,
) {
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
        val pulling = launch {
            pacing.follow {
                // The announcement of this device's own push comes while that push's run is going, before its answer
                // moves the device past it; once the run is over it has, and a pull would find nothing
                runs.running.first { !it }
                if (latest.get().isBehind(announced.get())) runs.request(SyncRequest())
            }
        }
        try {
            keepOpen(latest, pacing, announced)
        } finally {
            following.cancel()
            pulling.cancel()
        }
    }

    private suspend fun keepOpen(
        latest: AtomicReference<SyncConnectionState>,
        pacing: RunPacing,
        announced: AtomicReference<ServerIdentity?>,
    ) {
        var backoff = FIRST_BACKOFF
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
                        deviceChanges.hear(changes.devices)
                        val state = latest.get()
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
            if (heard.get()) backoff = FIRST_BACKOFF
            delay(backoff)
            backoff = (backoff * 2).coerceAtMost(MAX_BACKOFF)
        }
    }

    private fun SyncConnectionState.isBehind(server: ServerIdentity?) =
        server != null && (server.sequence > position || server.instanceId != instanceId)

    private fun SyncConnectionState.listens() = isLinked && stopped == null && stage == SyncStage.READY

    private companion object {
        val FIRST_BACKOFF: Duration = 2.seconds
        val MAX_BACKOFF: Duration = 5.minutes
    }
}
