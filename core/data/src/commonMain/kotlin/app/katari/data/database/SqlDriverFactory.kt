package app.katari.data.database

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import com.eygraber.sqldelight.androidx.driver.AndroidxSqliteConcurrencyModel.MultipleReadersSingleWriter
import com.eygraber.sqldelight.androidx.driver.AndroidxSqliteConfiguration
import com.eygraber.sqldelight.androidx.driver.AndroidxSqliteConnectionFactory
import com.eygraber.sqldelight.androidx.driver.AndroidxSqliteDatabaseType
import com.eygraber.sqldelight.androidx.driver.AndroidxSqliteDriver
import tachiyomi.data.Database

/**
 * Configured like Room's connection manager: WAL on every device, so a write never blocks reads, and a busy timeout
 * on every connection, so a connection waits for a lock held by another one instead of failing with SQLITE_BUSY.
 * Each reader connection keeps its own page cache, so [readerCount] trades memory for concurrent reads. Every
 * connection carries what [temporarySchema] publishes.
 */
internal fun configuredSqlDriver(
    databaseType: AndroidxSqliteDatabaseType,
    readerCount: Int,
    databaseRepository: DatabaseRepositoryImpl,
    temporarySchema: TemporarySchema,
): SqlDriver {
    return AndroidxSqliteDriver(
        connectionFactory = object : AndroidxSqliteConnectionFactory {
            override val driver: SQLiteDriver = BundledSQLiteDriver()

            override fun createConnection(name: String): SQLiteConnection {
                val connection = driver.open(name).apply {
                    execSQL("PRAGMA busy_timeout = 3000")
                }
                return TemporarySchemaConnection(connection, temporarySchema)
            }
        },
        databaseType = databaseType,
        schema = Database.Schema,
        configuration = AndroidxSqliteConfiguration(
            isForeignKeyConstraintsEnabled = true,
            concurrencyModel = MultipleReadersSingleWriter(isWal = true, walCount = readerCount),
        ),
        // onConfigure runs before the driver compares the schema version, and onOpen once the schema is ready
        onConfigure = {
            val version = executePragmaQuery("user_version", { QueryResult.Value(it.apply { next() }.getLong(0)) })
            databaseRepository.isMigrating.value = version in 1..<Database.Schema.version
        },
        onOpen = {
            databaseRepository.isMigrating.value = false
        },
    )
}
