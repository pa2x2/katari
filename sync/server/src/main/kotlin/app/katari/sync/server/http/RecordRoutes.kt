package app.katari.sync.server.http

import app.katari.sync.protocol.http.SyncHttp
import app.katari.sync.protocol.pull.GroupRequest
import app.katari.sync.protocol.pull.PullRequest
import app.katari.sync.protocol.push.PushRequest
import app.katari.sync.server.SyncServices
import app.katari.sync.server.record.PullOutcome
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal fun Route.recordRoutes(services: SyncServices) {
    post(SyncHttp.PUSH) {
        val request = call.receiveProto<PushRequest>(services.config.maxRequestBytes)
        RequestChecks.check(request)
        call.respondProto(withContext(Dispatchers.IO) { services.push.push(call.device, request) })
    }
    post(SyncHttp.PULL) {
        val request = call.receiveProto<PullRequest>(services.config.maxRequestBytes)
        RequestChecks.check(request)
        val outcome = withContext(Dispatchers.IO) {
            request.device?.let { services.devices.describe(call.device, it) }
            services.pull.pull(call.device, request)
        }
        when (outcome) {
            is PullOutcome.Page -> call.respondProto(outcome.page)
            is PullOutcome.Refused -> call.respondRefusal(outcome.code)
        }
    }
    post(SyncHttp.GROUP) {
        val request = call.receiveProto<GroupRequest>(services.config.maxRequestBytes)
        RequestChecks.check(request)
        call.respondProto(withContext(Dispatchers.IO) { services.groups.of(call.device.accountId, request.group) })
    }
    get(SyncHttp.SNAPSHOT) {
        call.respondProto(withContext(Dispatchers.IO) { services.snapshot.of(call.device.accountId) })
    }
}
