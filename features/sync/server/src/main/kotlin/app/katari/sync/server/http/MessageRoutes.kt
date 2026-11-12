package app.katari.sync.server.http

import app.katari.sync.protocol.http.SyncHttp
import app.katari.sync.protocol.message.FetchMessagesRequest
import app.katari.sync.protocol.message.SendMessageRequest
import app.katari.sync.server.SyncServices
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

internal fun Route.messageRoutes(services: SyncServices) {
    post(SyncHttp.SEND_MESSAGE) {
        val request = call.receiveProto<SendMessageRequest>(services.config.maxRequestBytes)
        RequestChecks.check(request)
        val response = withContext(Dispatchers.IO) { services.messages.send(call.device, request) }
        if (response == null) call.respond(HttpStatusCode.NotFound) else call.respondProto(response)
    }
    post(SyncHttp.FETCH_MESSAGES) {
        val request = call.receiveProto<FetchMessagesRequest>(services.config.maxRequestBytes)
        RequestChecks.check(request)
        val device = call.device.id
        val announced = services.changes.newestMessage(device)
        // Taken before the first look, so a message that comes in between ends the wait instead of being slept through
        val before = announced.value
        var page = withContext(Dispatchers.IO) { services.messages.fetch(device, request.after) }
        if (page.messages.isEmpty() && request.waitMillis > 0) {
            withTimeoutOrNull(request.waitMillis) { announced.first { it > before } }
            page = withContext(Dispatchers.IO) { services.messages.fetch(device, request.after) }
        }
        call.respondProto(page)
    }
}
