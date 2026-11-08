package app.katari.sync.server.http

import app.katari.sync.server.device.AuthenticationOutcome
import app.katari.sync.server.device.Device
import app.katari.sync.server.device.DeviceAuthentication
import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.RouteScopedPlugin
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.util.AttributeKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val AuthenticatedDeviceKey = AttributeKey<Device>("AuthenticatedDevice")

/** The device whose token signed the call; set on every route under [deviceAuthenticationPlugin]. */
internal val ApplicationCall.device: Device
    get() = attributes[AuthenticatedDeviceKey]

/** Refuses calls without the bearer token of a device that is linked, not revoked, and in an enabled account. */
internal fun deviceAuthenticationPlugin(authentication: DeviceAuthentication): RouteScopedPlugin<Unit> =
    createRouteScopedPlugin("DeviceAuthentication") {
        onCall { call ->
            val token = call.request.headers[HttpHeaders.Authorization]?.let(::bearerToken)
            when (val outcome = withContext(Dispatchers.IO) { authentication.authenticate(token) }) {
                is AuthenticationOutcome.Authenticated -> call.attributes.put(AuthenticatedDeviceKey, outcome.device)
                is AuthenticationOutcome.Refused -> call.respondRefusal(outcome.code)
            }
        }
    }

private fun bearerToken(header: String): String? {
    val (scheme, token) = header.trim().split(' ', limit = 2).takeIf { it.size == 2 } ?: return null
    return token.trim().takeIf { scheme.equals("Bearer", ignoreCase = true) && it.isNotEmpty() }
}
