package app.katari.sync.client

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.db.SqlDriver
import app.katari.sync.client.capture.SyncCapture
import app.katari.sync.client.capture.SyncCaptureSchemaSource
import app.katari.sync.client.outbox.OutboxDrain
import kotlinx.coroutines.flow.MutableStateFlow
import mihon.data.database.DatabaseRepositoryImpl
import mihon.data.database.TemporarySchema
import mihon.data.database.createSqlDriver
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.data.AndroidDatabaseHandler
import tachiyomi.data.Chapters
import tachiyomi.data.Database
import tachiyomi.data.DateColumnAdapter
import tachiyomi.data.Entries
import tachiyomi.data.History
import tachiyomi.data.MemoColumnAdapter
import tachiyomi.data.StringListColumnAdapter
import tachiyomi.data.UpdateStrategyColumnAdapter
import java.io.File

/** One device's database file, opened with the driver, adapters and capture wiring the app uses. */
internal class SyncTestDevice(file: File) : AutoCloseable {
    private val temporarySchema = TemporarySchema(SyncCaptureSchemaSource())
    val driver: SqlDriver = createSqlDriver(file, DatabaseRepositoryImpl(lazy { driver }), temporarySchema)
    val database = Database(
        driver = driver,
        entriesAdapter = Entries.Adapter(
            genreAdapter = StringListColumnAdapter,
            update_strategyAdapter = UpdateStrategyColumnAdapter,
            memoAdapter = MemoColumnAdapter,
        ),
        chaptersAdapter = Chapters.Adapter(memoAdapter = MemoColumnAdapter),
        historyAdapter = History.Adapter(last_readAdapter = DateColumnAdapter),
    )
    val handler = AndroidDatabaseHandler(database, driver)
    val defaultProfile = object : ActiveProfileProvider {
        override val activeProfileId = 1L
        override val activeProfileIdFlow = MutableStateFlow(1L)
    }
    val drain = OutboxDrain(handler)

    suspend fun startSync(deviceId: Long) {
        handler.await { sync_stateQueries.setServer(serverUrl = "https://sync.test", deviceId = deviceId) }
        SyncCapture(handler, temporarySchema).start()
    }

    suspend fun sql(vararg statements: String) {
        statements.forEach { driver.await(null, it, 0) }
    }

    suspend fun outboxKinds(): List<String> =
        handler.await { sync_outboxQueries.batch(Long.MAX_VALUE).awaitAsList().map { it.kind } }

    suspend fun clearOutbox() {
        handler.await { sync_outboxQueries.clear() }
    }

    suspend fun drainAll() {
        do {
            val drained = drain.drain()
        } while (drained > 0)
    }

    override fun close() = driver.close()
}
