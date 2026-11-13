package app.katari.sync.client

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.db.SqlDriver
import app.katari.data.database.DatabaseRepositoryImpl
import app.katari.data.database.TemporarySchema
import app.katari.data.database.createSqlDriver
import app.katari.profiles.core.ProfileDatabase
import app.katari.sync.client.apply.HeldRecords
import app.katari.sync.client.apply.IncomingRecords
import app.katari.sync.client.apply.SyncWriting
import app.katari.sync.client.capture.CaptureTriggers
import app.katari.sync.client.capture.SyncCapture
import app.katari.sync.client.connection.ThisDevice
import app.katari.sync.client.file.capture.FileCapture
import app.katari.sync.client.file.local.OwnedFiles
import app.katari.sync.client.firstsync.FirstSync
import app.katari.sync.client.kind.profile.LocalProfileDeletion
import app.katari.sync.client.kind.replacement.carry.MigrationCarryRules
import app.katari.sync.client.outbox.OutboxDrain
import app.katari.sync.client.part.ViewerSettingPreferences
import app.katari.sync.client.preference.SyncPreferences
import app.katari.sync.client.question.Questions
import app.katari.sync.client.readelsewhere.ChaptersReadElsewhere
import app.katari.sync.client.readelsewhere.ChaptersReadElsewhereListener
import app.katari.sync.client.record.PendingChanges
import app.katari.sync.client.setting.capture.SettingsCapture
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.setMain
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
import java.util.Collections

/**
 * One device's database file, opened with the driver, adapters and capture wiring the app uses, and its preferences
 * and files beside it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SyncTestDevice(file: File) : AutoCloseable {
    private val temporarySchema = TemporarySchema(CaptureTriggers.schemaPart().statements)
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
    val syncPreferences = SyncPreferences(preferences.profiles)
    val fileCapture = FileCapture(handler, ownedFiles, syncPreferences)

    // The app's viewers as the manga reader's double-tap animation, whose title values stand in for its setting
    private val viewerSettings = ViewerSettingPreferences { providerId, settingKey ->
        DOUBLE_TAP_ANIMATION_PREFERENCE.takeIf {
            providerId == MANGA_READER && settingKey == DOUBLE_TAP_ANIMATION
        }
    }
    val drain = OutboxDrain(handler, preferences.reads, syncPreferences, viewerSettings)

    // What the app's deletion does to the database and preferences; the test devices own no files
    private val profileDeletion = object : LocalProfileDeletion {
        override suspend fun delete(profileId: Long) {
            ProfileDatabase(handler).deleteProfile(profileId)
            preferences.profiles.deleteProfileState(profileId)
            preferences.sourceFiles.deleteProfile(profileId)
        }
    }

    // Migration as the app does it for a type whose read and bookmark marks both carry
    private val migrationRules = object : MigrationCarryRules {
        override fun chapterMarks(type: EntryType) = MigrationChildStateTransfer(read = true, bookmarks = true)
    }

    /** The app version this device runs, which a test changes to stand for an update. */
    @Volatile
    var appVersion = "test"

    private val thisDevice = object : ThisDevice {
        override fun platformName() = file.nameWithoutExtension

        override val platform = "Linux"

        override val appVersion get() = this@SyncTestDevice.appVersion
    }

    /** What sync told the app's listeners of chapters it made read here. */
    val readElsewhere: MutableList<ChaptersReadElsewhere> = Collections.synchronizedList(ArrayList())

    val writing = SyncWriting(
        handler,
        drain,
        preferences.writes,
        profileDeletion,
        migrationRules,
        preferences.profiles,
        thisDevice,
        syncPreferences,
        viewerSettings,
    ) { setOf(ChaptersReadElsewhereListener { readElsewhere += it }) }
    val incoming = IncomingRecords(writing)
    val pending = PendingChanges(handler)
    val questions = Questions(handler, writing)
    val held = HeldRecords(handler)
    val firstSync = FirstSync(handler, writing, preferences.reads, preferences.profiles, syncPreferences)

    suspend fun startSync(deviceId: Long) {
        handler.await { sync_stateQueries.setServer(serverUrl = "https://sync.test", deviceId = deviceId) }
        SyncCapture(handler, settingsCapture, fileCapture).start()
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

/** The test devices' one viewer setting: the manga reader's double-tap animation, as the app defines it. */
internal const val MANGA_READER = "builtin.manga.reader"
internal const val DOUBLE_TAP_ANIMATION = "double_tap_animation_millis"
internal const val DOUBLE_TAP_ANIMATION_PREFERENCE = "pref_double_tap_anim_speed"
