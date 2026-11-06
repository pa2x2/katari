package app.katari.sync.server.http

import app.katari.sync.protocol.account.RedeemInviteRequest
import app.katari.sync.protocol.device.RegisterDeviceRequest
import app.katari.sync.protocol.device.RevokeDeviceRequest
import app.katari.sync.protocol.error.SyncErrorCode
import app.katari.sync.protocol.http.SyncHttp
import app.katari.sync.server.SyncServices
import app.katari.sync.server.device.RegistrationOutcome
import app.katari.sync.server.invite.RedemptionOutcome
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Need no token: this is how a device gets one. */
internal fun Route.registrationRoutes(services: SyncServices) {
    post(SyncHttp.REGISTER) {
        val request = call.receiveProto<RegisterDeviceRequest>(services.config.maxRequestBytes)
        RequestChecks.check(request)
        val from = call.clientAddress(services.config.trustedProxies)
        when (val outcome = withContext(Dispatchers.IO) { services.registration.register(request, from) }) {
            is RegistrationOutcome.Registered -> call.respondProto(outcome.response)
            RegistrationOutcome.Refused -> call.respondRefusal(SyncErrorCode.UNAUTHORIZED)
            is RegistrationOutcome.Throttled -> call.respondThrottled(outcome.retryAfter)
        }
    }
    post(SyncHttp.REDEEM_INVITE) {
        val request = call.receiveProto<RedeemInviteRequest>(services.config.maxRequestBytes)
        RequestChecks.check(request)
        val from = call.clientAddress(services.config.trustedProxies)
        when (val outcome = withContext(Dispatchers.IO) { services.invites.redeem(request, from) }) {
            is RedemptionOutcome.Registered -> call.respondProto(outcome.response)
            RedemptionOutcome.Refused -> call.respondRefusal(SyncErrorCode.UNAUTHORIZED)
            is RedemptionOutcome.Throttled -> call.respondThrottled(outcome.retryAfter)
            RedemptionOutcome.UsernameTaken -> call.respondRefusal(SyncErrorCode.USERNAME_TAKEN)
            RedemptionOutcome.InvalidCredentials -> throw BadRequestException("Username or password breaks the rules")
        }
    }
}

internal fun Route.deviceRoutes(services: SyncServices) {
    get(SyncHttp.DEVICES) {
        call.respondProto(withContext(Dispatchers.IO) { services.devices.list(call.device.accountId) })
    }
    post(SyncHttp.LINK_CODE) {
        call.respondProto(withContext(Dispatchers.IO) { services.devices.issueLinkCode(call.device.accountId) })
    }
    post(SyncHttp.REVOKE) {
        val request = call.receiveProto<RevokeDeviceRequest>(services.config.maxRequestBytes)
        val revoked = withContext(Dispatchers.IO) { services.devices.revoke(call.device.accountId, request.device) }
        call.respond(if (revoked) HttpStatusCode.NoContent else HttpStatusCode.NotFound)
    }
}
