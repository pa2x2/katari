package app.katari.sync.server.storage.sqlite

import app.cash.sqldelight.Query
import app.cash.sqldelight.driver.jdbc.JdbcDriver
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.util.Properties
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * A driver over one SQLite file that many request threads write at once and that the recovery command's process may
 * open beside the server.
 *
 * SQLDelight's JdbcSqliteDriver opens a connection for every statement and begins deferred transactions. Two deferred
 * transactions that both read and then write cannot both upgrade to the write lock, and SQLite fails the loser with
 * SQLITE_BUSY at once instead of letting it wait, which two devices pushing together would hit. Here every transaction
 * begins IMMEDIATE (taking the write lock up front, so a second one waits on busy_timeout), and idle connections are
 * kept for reuse.
 */
internal class SqliteFileDriver(file: Path) : JdbcDriver() {
    private val url = "jdbc:sqlite:$file"
    private val properties = Properties().apply {
        setProperty("journal_mode", "WAL")
        setProperty("foreign_keys", "true")
        setProperty("transaction_mode", "IMMEDIATE")
        setProperty("busy_timeout", BUSY_TIMEOUT_MILLIS.toString())
    }
    private val idle = ConcurrentLinkedQueue<Connection>()

    override fun getConnection(): Connection = idle.poll() ?: DriverManager.getConnection(url, properties)

    override fun closeConnection(connection: Connection) {
        idle.offer(connection)
    }

    override fun close() {
        generateSequence { idle.poll() }.forEach(Connection::close)
    }

    override fun addListener(vararg queryKeys: String, listener: Query.Listener) = Unit

    override fun removeListener(vararg queryKeys: String, listener: Query.Listener) = Unit

    override fun notifyListeners(vararg queryKeys: String) = Unit

    private companion object {
        // Long enough to outlast any one transaction, which never waits on the network.
        const val BUSY_TIMEOUT_MILLIS = 30_000
    }
}
