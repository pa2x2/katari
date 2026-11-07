package app.katari.sync.client.trigger

import app.katari.sync.client.account.DeviceListChanges
import app.katari.sync.client.connection.SyncConnection
import app.katari.sync.client.connection.SyncConnectionState
import app.katari.sync.client.connection.SyncStage
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
import kotlinx.coroutines.launch
import mihon.core.platform.network.NetworkConnectivity
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * The live channel while the app is open (D021): announcements of a position past this device's start a run once they
 * pause ([PullBatching], D064), so another device's change shows within seconds, and its count of the account's
 * devices goes to [DeviceListChanges]. A channel that ends is opened again after a backoff that grows to
 * [MAX_BACKOFF] and starts over once the channel works; each end also starts a run, which finds out why when the
 * server refused it, as for a removed device.
 */
@Inject
class LiveUpdates(
    private val connection: SyncConnection,
    private val clients: SyncServerClients,
    private val network: NetworkConnectivity,
    private val runs: SyncRuns,
    private val deviceChanges: DeviceListChanges,
) {
    /** Whether the channel should be open: the app is in front, the device syncs, and there is a network. */
    fun wanted(foreground: Flow<Boolean>): Flow<Boolean> = combine(
        foreground,
        connection.states(),
        network.state(),
    ) { inFront, state, network -> inFront && state.listens() && network.hasNetwork }
        .distinctUntilChanged()

    /** Keeps the channel open until cancelled. */
    suspend fun listen(): Unit = coroutineScope {
        // Announcements arrive on OkHttp's thread; the position they are compared with is the newest one.
        val latest = AtomicReference(connection.state())
        val following = launch { connection.states().collect(latest::set) }
        val batching = PullBatching()
        // The announcement of this device's own push can come before the push's answer moves the device past it; by
        // the time the batch closes the answer has, and a pull would find nothing
        val announced = AtomicReference<ServerIdentity?>(null)
        val pulling = launch {
            batching.pulls { if (latest.get().isBehind(announced.get())) runs.request(SyncRequest()) }
        }
        try {
            keepOpen(latest, batching, announced)
        } finally {
            following.cancel()
            pulling.cancel()
        }
    }

    private suspend fun keepOpen(
        latest: AtomicReference<SyncConnectionState>,
        batching: PullBatching,
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
                        if (latest.get().isBehind(changes.server)) {
                            announced.set(changes.server)
                            batching.hear()
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
