package app.katari.sync.client

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.db.SqlDriver
import app.katari.sync.client.apply.HeldRecords
import app.katari.sync.client.apply.IncomingRecords
import app.katari.sync.client.apply.SyncWriting
import app.katari.sync.client.capture.SyncCapture
import app.katari.sync.client.outbox.OutboxDrain
import app.katari.sync.client.question.Questions
import app.katari.sync.client.record.PendingChanges
import app.katari.sync.client.setting.capture.SettingsCapture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.setMain
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

/**
 * One device's database file, opened with the driver, adapters and capture wiring the app uses, and its preferences
 * in files beside it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SyncTestDevice(file: File) : AutoCloseable {
    private val temporarySchema = TemporarySchema()
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
    private val scope = CoroutineScope(SupervisorJob()).also {
        // Source settings follow the active profile on the main thread, which tests don't have. Never reset: a device
        // closed while another is open would take the main thread from under the other's work.
        Dispatchers.setMain(Dispatchers.Unconfined)
    }
    val preferences =
        TestPreferenceStores(file.resolveSibling("${file.nameWithoutExtension}-preferences"), handler, scope)
    private val settingsCapture =
        SettingsCapture(handler, preferences.profiles, preferences.sourceFiles, preferences.reads)
    val drain = OutboxDrain(handler, preferences.reads)
    val writing = SyncWriting(handler, drain, preferences.writes, preferences.profiles)
    val incoming = IncomingRecords(writing)
    val pending = PendingChanges(handler)
    val questions = Questions(handler, writing)
    val held = HeldRecords(handler)

    suspend fun startSync(deviceId: Long) {
        handler.await { sync_stateQueries.setServer(serverUrl = "https://sync.test", deviceId = deviceId) }
        SyncCapture(handler, temporarySchema, settingsCapture).start()
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

    override fun close() {
        settingsCapture.stop()
        scope.cancel()
        driver.close()
    }
}
