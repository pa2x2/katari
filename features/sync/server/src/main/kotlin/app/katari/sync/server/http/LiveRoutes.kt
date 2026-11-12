package app.katari.sync.server.http

import app.katari.sync.protocol.http.SyncHttp
import app.katari.sync.protocol.live.ChangesAvailable
import app.katari.sync.protocol.server.ServerIdentity
import app.katari.sync.server.SyncServices
import app.katari.sync.server.device.Device
import app.katari.sync.server.record.identityOf
import io.ktor.server.routing.Route
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf

internal fun Route.liveRoute(services: SyncServices) {
    webSocket(SyncHttp.LIVE) {
        val device = call.device
        services.connected.opened(device.id)
        val closing = launch {
            val reason = merge(
                services.changes.revocations.filter { it == device.id }.map { "Device revoked" },
                services.changes.disabledAccounts.filter { it == device.accountId }.map { "Account disabled" },
            ).first()
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, reason))
        }
        val notices = launch {
            val devices = services.changes.devices(device.accountId)
            val messages = services.changes.newestMessage(device.id)
            // Taken before the storage is read, so what comes in between is announced again rather than missed
            var sentDevices = devices.value
            var sentMessage = messages.value
            var sent = withContext(Dispatchers.IO) { services.storage.identityOf(device.accountId) }
            announce(services, device, sent, sentDevices)
            combine(services.changes.sequence(device.accountId), devices, messages, ::Triple)
                .collect { (sequence, deviceCount, newestMessage) ->
                    if (sequence > sent.sequence || deviceCount != sentDevices || newestMessage > sentMessage) {
                        if (sequence > sent.sequence) {
                            sent = withContext(Dispatchers.IO) { services.storage.identityOf(device.accountId) }
                        }
                        sentDevices = deviceCount
                        sentMessage = newestMessage
                        announce(services, device, sent, sentDevices)
                    }
                }
        }
        try {
            // Devices send nothing; this waits for the connection to close.
            incoming.consumeEach { }
        } finally {
            closing.cancel()
            notices.cancel()
            services.connected.closed(device.id)
        }
    }
}

/** Every announcement carries what waits for the device, so one that went unheard is made up by the next. */
private suspend fun DefaultWebSocketServerSession.announce(
    services: SyncServices,
    device: Device,
    server: ServerIdentity,
    devices: Long,
) {
    val waiting = withContext(Dispatchers.IO) { services.messages.waitingFor(device.id) }
    val changes = ChangesAvailable(
        server = server,
        devices = devices,
        newestMessage = waiting.newest,
        messages = waiting.messages,
    )
    send(Frame.Binary(true, ProtoBuf.encodeToByteArray(changes)))
}
