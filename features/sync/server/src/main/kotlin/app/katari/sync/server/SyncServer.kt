package app.katari.sync.server

import app.katari.sync.server.config.ServerConfig
import app.katari.sync.server.http.syncApi
import app.katari.sync.server.storage.sqlite.SqliteServerStorage
import app.katari.sync.server.web.webPages
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.util.logging.KtorSimpleLogger
import kotlinx.coroutines.runBlocking
import java.security.SecureRandom
import kotlin.io.path.createDirectories
import kotlin.time.Clock

/**
 * The server over [config]'s data directory: the sync API and the web page on one port. [main] serves until the
 * process is stopped; apps' tests start one in their own process on a free port ([ServerConfig.port] 0). A run whose
 * every code, token and time must come out the same each time, such as the showcase's, passes a [clock] of its own and
 * a seeded [random].
 */
class SyncServer(
    private val config: ServerConfig,
    clock: Clock = Clock.System,
    random: SecureRandom = SecureRandom(),
) : AutoCloseable {
    private val log = KtorSimpleLogger("app.katari.sync.server")
    private val storage = config.dataDirectory.createDirectories().let { SqliteServerStorage.open(config.databaseFile) }
    val services = SyncServices(config, storage, clock, random).also { it.content.prepare() }

    // CIO rather than Netty: pure Kotlin with a fraction of the dependencies, which keeps the image small and the same
    // on amd64 and arm64. It speaks HTTP/1.1 only, which is all a server behind a reverse proxy needs.
    private val server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration> =
        embeddedServer(CIO, port = config.port, host = config.host, watchPaths = emptyList()) {
            syncApi(services)
            webPages(services)
            val retention = services.retention.start(this)
            monitor.subscribe(ApplicationStopped) {
                retention.cancel()
                storage.close()
            }
        }

    /** The port it listens on, the one picked for it when [ServerConfig.port] was 0. */
    val port: Int
        get() = runBlocking { server.engine.resolvedConnectors().first().port }

    /** Starts serving; with [wait], returns only once the server stops. */
    fun start(wait: Boolean) {
        log.info(
            "Katari sync server ${ServerVersion.name}, data directory ${config.dataDirectory}, " +
                "instance ${storage.instanceId}",
        )
        services.firstAdminSetup.setupCode?.let { code ->
            log.info("No accounts yet. The setup code for creating the admin account is $code")
        }
        server.start(wait = wait)
    }

    override fun close() {
        server.stop(gracePeriodMillis = 0, timeoutMillis = STOP_TIMEOUT_MILLIS)
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

/** Serves until the process is stopped. */
internal fun serve(config: ServerConfig) {
    SyncServer(config).start(wait = true)
}
