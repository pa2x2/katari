package app.katari.sync.server.http

import app.katari.sync.protocol.http.SyncHttp
import app.katari.sync.protocol.server.ServerInfo
import app.katari.sync.server.SyncServices
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Health check for the container, answering once the database can be read. */
internal const val HEALTH_PATH = "/health"

/** Reachable without the version header or a token. */
internal fun Route.infoRoutes(services: SyncServices) {
    get(SyncHttp.INFO) {
        call.respondProto(
            ServerInfo(
                instanceId = services.storage.instanceId,
                protocolVersions = SupportedProtocolVersions,
                needsSetup = services.firstAdminSetup.setupCode != null,
            ),
        )
    }
    get(HEALTH_PATH) {
        withContext(Dispatchers.IO) { services.storage.accounts.count() }
        call.respondText("ok")
    }
}
