package app.katari.sync.client.connection

import app.katari.sync.data.Sync_state
import app.katari.sync.protocol.device.DeviceId
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** This device's connection to its sync server, as `sync_state` keeps it. */
data class SyncConnectionState(
    /** Whether changes are captured, which they are from signing in until the device leaves its account. */
    val enabled: Boolean,
    val device: DeviceId?,
    val server: HttpUrl?,
    val accountName: String?,
    /** The instance the position belongs to ([app.katari.sync.protocol.server.ServerIdentity.instanceId]). */
    val instanceId: String?,
    val position: Long,
    val chain: String,
    val stage: SyncStage,
    /** The server sequence a download or resync runs to, as the server last reported it; 0 when none runs. */
    val stageTarget: Long,
    val stopped: SyncStop?,
    /** The protocol version the app had when [stopped] for a version mismatch. */
    val stoppedProtocol: Int,
    /** Epoch milliseconds. */
    val lastSyncedAt: Long?,
    /** Epoch milliseconds of the first failure to reach the server since it was last reached. */
    val unreachableSince: Long?,
    /** The last message from another device handled here; only messages numbered past it are new. */
    val messagesHandled: Long,
) {
    /** Whether the device belongs to an account, signed in and registered, whether or not it syncs right now. */
    val isLinked: Boolean get() = enabled && device != null && server != null

    internal companion object {
        fun of(row: Sync_state) = SyncConnectionState(
            enabled = row.enabled,
            device = row.device_id?.let(::DeviceId),
            server = row.server_url?.toHttpUrlOrNull(),
            accountName = row.account_name,
            instanceId = row.server_instance_id,
            position = row.cursor,
            chain = row.chain,
            stage = SyncStage.of(row.stage),
            stageTarget = row.stage_target,
            stopped = SyncStop.of(row.stopped),
            stoppedProtocol = row.stopped_protocol.toInt(),
            lastSyncedAt = row.last_synced_at,
            unreachableSince = row.unreachable_since,
            messagesHandled = row.messages_handled,
        )
    }
}
