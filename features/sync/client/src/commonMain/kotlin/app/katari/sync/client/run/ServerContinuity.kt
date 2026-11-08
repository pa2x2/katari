package app.katari.sync.client.run

import app.katari.sync.client.connection.SyncConnection
import app.katari.sync.client.connection.SyncStage
import app.katari.sync.protocol.server.ServerIdentity
import dev.zacsweers.metro.Inject
import tachiyomi.data.DatabaseHandler

/**
 * Whether the server answering a pull or a push is still the one this device synced with (D048). It isn't when it is
 * another instance, or when its sequence is below what it reached before: the device's position, or a copy it
 * acknowledged or sent. The acknowledged copies matter because a device's own pushes are past its position until its
 * next pull, so a server restored to a copy from just before them still holds the position and its chain.
 */
@Inject
class ServerContinuity(
    private val handler: DatabaseHandler,
    private val connection: SyncConnection,
) {
    /** Throws [ResyncNeeded] when [server] isn't the one the device synced with. */
    suspend fun check(server: ServerIdentity) {
        val state = connection.state()
        if (state.instanceId != null && server.instanceId != state.instanceId) {
            throw ResyncNeeded(SyncStage.RESYNC, server)
        }
        val reached = maxOf(state.position, handler.awaitOne { sync_recordsQueries.newestBase() })
        if (server.sequence < reached) throw ResyncNeeded(SyncStage.RESYNC, server)
    }
}
