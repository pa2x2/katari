package mihon.data.database

import android.app.ActivityManager
import android.content.Context
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import app.cash.sqldelight.db.SqlDriver
import com.eygraber.sqldelight.androidx.driver.AndroidxSqliteConcurrencyModel.MultipleReadersSingleWriter
import com.eygraber.sqldelight.androidx.driver.AndroidxSqliteConfiguration
import com.eygraber.sqldelight.androidx.driver.AndroidxSqliteConnectionFactory
import com.eygraber.sqldelight.androidx.driver.AndroidxSqliteDatabaseType
import com.eygraber.sqldelight.androidx.driver.AndroidxSqliteDriver
import com.eygraber.sqldelight.androidx.driver.FileProvider
import tachiyomi.data.Database

/**
 * Configured like Room's connection manager: WAL on every device, so a write never blocks reads, and a busy timeout
 * on every connection, so a connection waits for a lock held by another one instead of failing with SQLITE_BUSY.
 * Low-RAM devices get one reader instead of four, since each connection keeps its own page cache.
 */
fun createSqlDriver(context: Context): SqlDriver {
    val isLowRam = context.getSystemService(ActivityManager::class.java)?.isLowRamDevice == true
    return AndroidxSqliteDriver(
        connectionFactory = object : AndroidxSqliteConnectionFactory {
            override val driver: SQLiteDriver = BundledSQLiteDriver()

            override fun createConnection(name: String): SQLiteConnection {
                return driver.open(name).apply {
                    execSQL("PRAGMA busy_timeout = 3000")
                }
            }
        },
        databaseType = AndroidxSqliteDatabaseType.FileProvider(context, "tachiyomi.db"),
        schema = Database.Schema,
        configuration = AndroidxSqliteConfiguration(
            isForeignKeyConstraintsEnabled = true,
            concurrencyModel = MultipleReadersSingleWriter(isWal = true, walCount = if (isLowRam) 1 else 4),
        ),
    )
}
