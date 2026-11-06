package app.katari.sync.client

import app.katari.sync.server.SyncServer
import app.katari.sync.server.account.FirstAdminOutcome
import app.katari.sync.server.config.ServerConfig
import java.io.File
import java.net.ServerSocket

/**
 * A real sync server in the test's process, over SQLite in [directory], always on one address: restarting it over a
 * copy of its data or over none stands for a server restored from a backup or replaced by a new one.
 */
internal class TestSyncServer(private val directory: File, private val pageSize: Int = 3) : AutoCloseable {
    private val port = ServerSocket(0).use { it.localPort }
    private var data = directory.resolve("server")
    private var server: SyncServer? = null

    val address = "http://127.0.0.1:$port"

    init {
        start()
    }

    /**
     * Creates an account its devices join by link code, and returns its ID. A server's first account is its admin,
     * made with the setup code as a person would make it, since the server asks apps to wait until then.
     */
    fun account(username: String): Long = running().services.let { services ->
        val setupCode = services.firstAdminSetup.setupCode
        if (setupCode == null) {
            services.storage.accounts.create(username, "", false, services.clock.now().toEpochMilliseconds()).id
        } else {
            val created = services.firstAdminSetup.createAdmin(setupCode, username, "a long password", "127.0.0.1")
            (created as FirstAdminOutcome.Created).account.id
        }
    }

    fun linkCode(accountId: Long): String = running().services.devices.issueLinkCode(accountId).code

    /** A copy of the server's data as it is now, to [restoreFrom] later. */
    fun backUp(): File {
        stop()
        val copy = directory.resolve("backup-${System.nanoTime()}")
        data.copyRecursively(copy)
        start()
        return copy
    }

    fun restoreFrom(backup: File) {
        stop()
        data.deleteRecursively()
        backup.copyRecursively(data)
        start()
    }

    /** A new server with no data under the same address. */
    fun replaceWithEmpty() {
        stop()
        data = directory.resolve("server-${System.nanoTime()}")
        start()
    }

    private fun start() {
        val config = ServerConfig.fromEnvironment(mapOf(ServerConfig.PULL_PAGE_SIZE to "$pageSize"))
            .copy(dataDirectory = data.toPath(), host = "127.0.0.1", port = port)
        server = SyncServer(config).also { it.start(wait = false) }
    }

    private fun stop() {
        server?.close()
        server = null
    }

    private fun running() = checkNotNull(server)

    override fun close() = stop()
}
