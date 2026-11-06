package app.katari.sync.server.http

import app.katari.sync.protocol.http.SyncHttp
import app.katari.sync.protocol.live.ChangesAvailable
import app.katari.sync.server.SyncServices
import app.katari.sync.server.record.identityOf
import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.consumeEach
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
        val closing = launch {
            val reason = merge(
                services.changes.revocations.filter { it == device.id }.map { "Device revoked" },
                services.changes.disabledAccounts.filter { it == device.accountId }.map { "Account disabled" },
            ).first()
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, reason))
        }
        val notices = launch {
            var sent = withContext(Dispatchers.IO) { services.storage.identityOf(device.accountId) }
            send(Frame.Binary(true, ProtoBuf.encodeToByteArray(ChangesAvailable(sent))))
            services.changes.sequence(device.accountId).collect { sequence ->
                if (sequence > sent.sequence) {
                    sent = withContext(Dispatchers.IO) { services.storage.identityOf(device.accountId) }
                    send(Frame.Binary(true, ProtoBuf.encodeToByteArray(ChangesAvailable(sent))))
                }
            }
        }
        try {
            // Devices send nothing; this waits for the connection to close.
            incoming.consumeEach { }
        } finally {
            closing.cancel()
            notices.cancel()
        }
    }
}
