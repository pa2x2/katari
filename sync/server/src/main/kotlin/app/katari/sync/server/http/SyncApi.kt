package app.katari.sync.server.http

import app.katari.sync.server.SyncServices
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.compression.deflate
import io.ktor.server.plugins.compression.gzip
import io.ktor.server.plugins.compression.matchContentType
import io.ktor.server.routing.Route
import io.ktor.server.routing.RouteSelector
import io.ktor.server.routing.RouteSelectorEvaluation
import io.ktor.server.routing.RoutingResolveContext
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import kotlin.time.Duration.Companion.seconds

/** The sync API: the protocol of :sync:protocol over HTTP, as [app.katari.sync.protocol.http.SyncHttp] lays it out. */
fun Application.syncApi(services: SyncServices) {
    install(Compression) {
        gzip { matchContentType(ProtobufContentType) }
        deflate { matchContentType(ProtobufContentType) }
        maxDecodedContentLength = maxOf(services.config.maxRequestBytes, services.config.maxFileBytes)
    }
    install(WebSockets) {
        // Under the idle timeouts of common reverse proxies, so a quiet live channel stays open.
        pingPeriodMillis = 30.seconds.inWholeMilliseconds
        timeoutMillis = 15.seconds.inWholeMilliseconds
        maxFrameSize = 64 * 1024
    }
    routing {
        infoRoutes(services)
        group("protocol version") {
            install(ProtocolVersionCheck)
            registrationRoute(services)
            group("device") {
                install(deviceAuthenticationPlugin(services.authentication))
                deviceRoutes(services)
                recordRoutes(services)
                fileRoutes(services)
                liveRoute(services)
            }
        }
    }
}

/** Routes under it share its plugins without adding to their paths. */
private fun Route.group(name: String, build: Route.() -> Unit): Route = createChild(GroupSelector(name)).apply(build)

private class GroupSelector(private val name: String) : RouteSelector() {
    override suspend fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation =
        RouteSelectorEvaluation.Transparent

    override fun toString(): String = "($name)"
}
