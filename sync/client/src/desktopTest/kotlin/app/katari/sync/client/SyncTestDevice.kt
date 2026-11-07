package app.katari.sync.client

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.db.SqlDriver
import app.katari.sync.client.apply.HeldRecords
import app.katari.sync.client.apply.IncomingRecords
import app.katari.sync.client.apply.SyncWriting
import app.katari.sync.client.capture.SyncCapture
import app.katari.sync.client.connection.ThisDevice
import app.katari.sync.client.file.capture.FileCapture
import app.katari.sync.client.file.local.OwnedFiles
import app.katari.sync.client.firstsync.FirstSync
import app.katari.sync.client.kind.profile.LocalProfileDeletion
import app.katari.sync.client.kind.replacement.carry.MigrationCarryRules
import app.katari.sync.client.outbox.OutboxDrain
import app.katari.sync.client.question.Questions
import app.katari.sync.client.record.PendingChanges
import app.katari.sync.client.restore.RestoreStamping
import app.katari.sync.client.setting.capture.SettingsCapture
import app.katari.sync.protocol.device.DeviceDescription
import eu.kanade.tachiyomi.source.entry.EntryType
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
import mihon.feature.profiles.core.ProfileDatabase
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
import tachiyomi.domain.entry.migration.MigrationChildStateTransfer
import java.io.File

/**
 * One device's database file, opened with the driver, adapters and capture wiring the app uses, and its preferences
 * and files beside it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SyncTestDevice(file: File) : AutoCloseable {
    val temporarySchema = TemporarySchema()
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
    val settingsCapture =
        SettingsCapture(handler, preferences.profiles, preferences.sourceFiles, preferences.reads)
    val files = TestFileStore(file.resolveSibling("${file.nameWithoutExtension}-files"), scope)
    val ownedFiles = OwnedFiles(files)
    val fileCapture = FileCapture(handler, ownedFiles)
    val drain = OutboxDrain(handler, preferences.reads)

    // What the app's deletion does to the database and preferences; the test devices own no files
    private val profileDeletion = object : LocalProfileDeletion {
        override suspend fun delete(profileId: Long) {
            ProfileDatabase(handler).deleteProfile(profileId)
            preferences.profiles.deleteProfileState(profileId)
            preferences.sourceFiles.deleteProfile(profileId)
        }
    }

    // Migration as the app does it for a type whose read and bookmark marks both carry, copying trackers as they are
    private val migrationRules = object : MigrationCarryRules {
        override fun chapterMarks(type: EntryType) = MigrationChildStateTransfer(read = true, bookmarks = true)

        override fun copiesTrackerBinding(trackerId: Long) = true
    }

    /** The app version this device runs, which a test changes to stand for an update. */
    @Volatile
    var appVersion = "test"

    private val thisDevice = object : ThisDevice {
        override fun description() = DeviceDescription(name = file.nameWithoutExtension, platform = "Linux", appVersion)

        override val appVersion get() = this@SyncTestDevice.appVersion
    }

    val writing = SyncWriting(
        handler,
        drain,
        preferences.writes,
        profileDeletion,
        migrationRules,
        preferences.profiles,
        thisDevice,
    )
    val incoming = IncomingRecords(writing)
    val pending = PendingChanges(handler)
    val questions = Questions(handler, writing)
    val held = HeldRecords(handler)
    val firstSync = FirstSync(handler, writing, preferences.reads, preferences.profiles)
    val restoreStamping = RestoreStamping(handler, writing, settingsCapture, preferences.reads)

    suspend fun startSync(deviceId: Long) {
        handler.await { sync_stateQueries.setServer(serverUrl = "https://sync.test", deviceId = deviceId) }
        SyncCapture(handler, temporarySchema, settingsCapture, fileCapture).start()
    }

    suspend fun sql(vararg statements: String) {
        statements.forEach { driver.await(null, it, 0) }
    }

    suspend fun entryId(url: String): Long = rows("SELECT _id FROM entries WHERE url = '$url'").single().toLong()

    /** Sets the entry's custom cover as the person does from the entry's cover screen. */
    suspend fun setCustomCover(url: String, bytes: ByteArray) = files.setCover(entryId(url), bytes)

    suspend fun customCover(url: String): ByteArray? = files.cover(entryId(url))

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
        fileCapture.stop()
        settingsCapture.stop()
        scope.cancel()
        driver.close()
    }
}
