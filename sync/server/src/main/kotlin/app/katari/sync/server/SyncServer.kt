package app.katari.sync.server

import app.katari.sync.server.config.ServerConfig
import app.katari.sync.server.http.syncApi
import app.katari.sync.server.storage.sqlite.SqliteServerStorage
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.util.logging.KtorSimpleLogger
import kotlin.io.path.createDirectories

/** Serves until the process is stopped. */
internal fun serve(config: ServerConfig) {
    val log = KtorSimpleLogger("app.katari.sync.server")
    config.dataDirectory.createDirectories()
    val storage = SqliteServerStorage.open(config.databaseFile)
    val services = SyncServices(config, storage)
    services.content.prepare()
    log.info("Data directory ${config.dataDirectory}, instance ${storage.instanceId}")
    services.firstAdminSetup.setupCode?.let { code ->
        log.info("No accounts yet. The setup code for creating the admin account is $code")
    }

    // CIO rather than Netty: pure Kotlin with a fraction of the dependencies, which keeps the image small and the same
    // on amd64 and arm64. It speaks HTTP/1.1 only, which is all a server behind a reverse proxy needs.
    embeddedServer(CIO, port = config.port, host = config.host, watchPaths = emptyList()) {
        syncApi(services)
        val retention = services.retention.start(this)
        monitor.subscribe(ApplicationStopped) {
            retention.cancel()
            storage.close()
        }
    }.start(wait = true)
}
