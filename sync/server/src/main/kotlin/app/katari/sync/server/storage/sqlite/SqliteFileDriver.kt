package app.katari.sync.server.storage.sqlite

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.driver.jdbc.JdbcCursor
import app.cash.sqldelight.driver.jdbc.JdbcDriver
import app.cash.sqldelight.driver.jdbc.JdbcPreparedStatement
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap
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
 *
 * JdbcDriver also prepares every statement anew and closes it after one run, which costs more than running the small
 * statements of a push. Here each connection keeps its statements prepared, by SQLDelight's query identifier; queries
 * without one (those whose SQL varies, such as IN lists) are prepared each time as before. Their number is bounded by
 * the queries in the schema, so nothing is evicted.
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

    // A connection is used by one thread at a time, and handing it over through [idle] publishes its statements.
    private val prepared = ConcurrentHashMap<Connection, MutableMap<Int, PreparedStatement>>()

    override fun getConnection(): Connection = idle.poll() ?: DriverManager.getConnection(url, properties)

    override fun closeConnection(connection: Connection) {
        idle.offer(connection)
    }

    override fun close() {
        generateSequence { idle.poll() }.forEach(Connection::close)
        prepared.clear()
    }

    override fun execute(
        identifier: Int?,
        sql: String,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?,
    ): QueryResult<Long> {
        if (identifier == null) return super.execute(identifier, sql, parameters, binders)
        return withPrepared(identifier, sql) { statement ->
            QueryResult.Value(JdbcPreparedStatement(statement).apply { binders?.invoke(this) }.execute())
        }
    }

    override fun <R> executeQuery(
        identifier: Int?,
        sql: String,
        mapper: (SqlCursor) -> QueryResult<R>,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?,
    ): QueryResult<R> {
        if (identifier == null) return super.executeQuery(identifier, sql, mapper, parameters, binders)
        return withPrepared(identifier, sql) { statement ->
            JdbcPreparedStatement(statement).apply { binders?.invoke(this) }
            statement.executeQuery().use { mapper(JdbcCursor(it)) }
        }
    }

    // The JDBC driver resets a statement before each run. One that failed is dropped instead of kept.
    private inline fun <T> withPrepared(identifier: Int, sql: String, run: (PreparedStatement) -> T): T {
        val (connection, onClose) = connectionAndClose()
        try {
            val statements = prepared.getOrPut(connection) { HashMap() }
            val statement = statements.getOrPut(identifier) { connection.prepareStatement(sql) }
            try {
                return run(statement)
            } catch (e: Throwable) {
                statements.remove(identifier)
                statement.close()
                throw e
            }
        } finally {
            onClose()
        }
    }

    override fun addListener(vararg queryKeys: String, listener: Query.Listener) = Unit

    override fun removeListener(vararg queryKeys: String, listener: Query.Listener) = Unit

    override fun notifyListeners(vararg queryKeys: String) = Unit

    private companion object {
        // Long enough to outlast any one transaction, which never waits on the network.
        const val BUSY_TIMEOUT_MILLIS = 30_000
    }
}
