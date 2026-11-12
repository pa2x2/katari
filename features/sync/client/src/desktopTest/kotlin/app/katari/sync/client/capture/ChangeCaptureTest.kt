package app.katari.sync.client.capture

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.katari.sync.client.SyncTestDevice
import app.katari.sync.client.kind.SyncKinds
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tachiyomi.data.category.CategoryRepositoryImpl
import tachiyomi.data.entry.EntryChapterRepositoryImpl
import tachiyomi.data.entry.EntryProgressRepositoryImpl
import tachiyomi.data.entry.EntryRepositoryImpl
import tachiyomi.data.history.HistoryRepositoryImpl
import tachiyomi.data.library.update.LibraryUpdateReportRepositoryImpl
import tachiyomi.domain.category.model.NewCategory
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressLocator
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.history.model.HistoryUpdate
import tachiyomi.domain.history.model.activity.HistoryCompletionCause
import tachiyomi.domain.history.model.activity.HistoryCompletionUpdate
import tachiyomi.domain.library.update.model.LibraryUpdateTrigger
import java.io.File
import java.util.Date

class ChangeCaptureTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `writes are captured only while sync is on, and refreshes and cache removal are no change`() = runTest {
        SyncTestDevice(directory.resolve("device.db")).use { device ->
            device.sql(
                "INSERT INTO source_identities(local_key, id_scheme, external_id) VALUES (-2, 'aniyomi', 777)",
                "INSERT INTO profiles(_id, uuid, name, color_seed, position) VALUES (2, 'second', 'Second', 0, 1)",
                """
                INSERT INTO entries(_id, profile_id, source, url, title, favorite, date_added, type)
                VALUES (1, 1, -2, '/show', 'Show', 1, 1000, 'anime'), (2, 1, -2, '/other', 'Other', 1, 0, 'anime'),
                    (3, 1, 0, 'folder', 'Local', 1, 0, 'manga')
                """,
                "INSERT INTO chapters(_id, entry_id, url, name) VALUES (10, 1, '/show/1', 'Episode 1')",
                "INSERT INTO chapters(_id, entry_id, url, name) VALUES (12, 1, '/show/0', 'Recap')",
                "INSERT INTO chapters(_id, entry_id, url, name) VALUES (11, 3, 'folder/1', 'Local chapter')",
            )
            val entries = EntryRepositoryImpl(device.handler, device.defaultProfile)
            val chapters = EntryChapterRepositoryImpl(device.handler, device.defaultProfile)
            val categories = CategoryRepositoryImpl(device.handler, device.defaultProfile)
            val history = HistoryRepositoryImpl(device.handler, device.defaultProfile)
            val progress = EntryProgressRepositoryImpl(device.handler)
            val db = device.database

            entries.updateNotes(1, 1, "Written before sync")
            chapters.update(chapters.getChapterById(10)!!.copy(read = true))
            categories.insert(NewCategory("Before sync", 0))
            device.outboxKinds().shouldBeEmpty()

            device.startSync(deviceId = 1)
            suspend fun expect(write: String, vararg kinds: String, perform: suspend () -> Unit) {
                device.clearOutbox()
                perform()
                withClue(write) { device.outboxKinds().sorted() shouldBe kinds.sorted() }
                device.drainAll()
            }

            expect("rename profile", "profile") {
                db.profilesQueries.update(
                    name = "Renamed",
                    colorSeed = null,
                    position = null,
                    requiresAuth = null,
                    isArchived = null,
                    id = 2,
                )
            }
            expect("entry notes", "entry") { entries.updateNotes(1, 1, "Notes") }
            expect("chapter unread", "chapter") {
                chapters.update(chapters.getChapterById(10)!!.copy(read = false))
            }
            expect("local-source chapter read", "chapter") {
                chapters.update(chapters.getChapterById(11)!!.copy(read = true))
            }
            expect("progress", "progress") {
                progress.upsert(
                    EntryProgressState(
                        entryId = 1,
                        chapterId = 10,
                        resourceKey = "/show/1",
                        locator = EntryProgressLocator(kind = "time", position = 40),
                        locatorUpdatedAt = 1,
                    ),
                )
            }
            expect("history", "history", "history") {
                history.upsertHistory(HistoryUpdate(chapterId = 10, readAt = Date(5), sessionReadDuration = 60))
                history.upsertHistory(HistoryUpdate(chapterId = 12, readAt = Date(6), sessionReadDuration = 60))
            }
            expect("activity session and segment", "activity.session", "activity.segment") {
                db.activityQueries.insertActivitySession("session", 1, 100, 200)
                db.activityQueries.upsertActivitySegment("session", 10, "2026-11-06", "UTC", 100, 200, 100)
            }
            // The first activity of a profile also starts its statistics.
            expect("completion", "activity.completion", "statistics.epoch") {
                history.recordCompletion(
                    HistoryCompletionUpdate(
                        eventId = "event",
                        entryId = 1,
                        chapterId = 10,
                        sessionId = "session",
                        occurredAtEpochMillis = 200,
                        localDate = "2026-11-06",
                        timeZoneId = "UTC",
                        cause = HistoryCompletionCause.MANUAL,
                    ),
                )
            }
            expect("statistics reset", "statistics.epoch") {
                db.activityQueries.resetStatisticsEpoch(1, 300)
            }
            expect("library update started and finished", "library.update", "library.update") {
                val reports = LibraryUpdateReportRepositoryImpl(device.handler, device.defaultProfile)
                reports.startRun(1, startedAt = 100, trigger = LibraryUpdateTrigger.AUTOMATIC, librarySize = 2)
                reports.finishRun(1, startedAt = 100, finishedAt = 200)
            }
            expect("category", "category") { categories.insert(NewCategory("Watching", 0)) }
            expect("category membership", "category.membership") {
                entries.setCategories(1, listOf(categories.getAll(1).single { it.name == "Watching" }.id))
            }
            expect("category update rules", "category.update-rules") {
                db.category_update_rulesQueries.upsert(1, 0, false, true, true, false, false, false, 12)
            }
            expect("entry update mode", "entry.update-mode") {
                db.entry_update_modesQueries.upsert(1, "never")
            }
            expect("excluded scanlator", "entry.excluded-scanlator") {
                db.excluded_scanlatorsQueries.insert(1, 1, "Group")
            }
            expect("viewer setting override", "entry.viewer-setting") {
                db.viewer_setting_overridesQueries.upsert(1, "player", "speed", "1.5", 0)
            }
            expect("download preferences", "entry.download-preferences") {
                db.download_preferencesQueries.upsertInsert(1, "dub", null, null, null, null, "balanced", 0)
            }
            expect("translation languages", "entry.translation-languages") {
                db.entry_translation_languagesQueries.upsert(1, "ja", "en", false, 0)
            }
            expect("intro times pick", "entry.intro-times-pick") {
                db.intro_times_matchesQueries.upsert(1, "", 5, null, "Show", 2020, true, 0, null, null)
            }
            expect("theintrodb pick", "entry.theintrodb-pick") {
                db.theintrodb_matchesQueries.upsert(1, "", 5, null, null, "Show", 2020, 1, 1, true, 0)
            }
            expect("playback preferences", "entry.playback-preferences") {
                db.playback_preferencesQueries.upsertInsert(
                    1, "dub", null, null, null, null, "auto", null, null, null, null, null, null, null, null, null,
                    null, false, null, null, null, null, 0,
                )
            }
            expect("user merge", "merge.member", "merge.member") {
                db.merged_entriesQueries.insert(1, 1, 1, 0)
                db.merged_entriesQueries.insert(1, 1, 2, 1)
            }
            expect("replacing migration", "entry.replacement") {
                db.entry_migration_operationsQueries.insert("operation", "fingerprint", 1, 2, 1, "REPLACE", false, 0)
            }
            expect("extension repository", "extension.repository") {
                db.extension_storeQueries.upsert(
                    indexUrl = "https://repo/index.min.json",
                    name = "Repo",
                    badgeLabel = "R",
                    signingKey = null,
                    contactWebsite = "https://repo",
                    contactDiscord = null,
                    format = "tachiyomi",
                    extensionListUrl = null,
                )
            }
            expect("install and uninstall an extension", "extension.installed", "extension.installed") {
                db.extension_installsQueries.set("eu.kanade.extension.show", installed = true, changedAt = 1000)
                db.extension_installsQueries.set("eu.kanade.extension.show", installed = false, changedAt = 2000)
            }
            expect("move to another profile", "entry", "entry.replacement") {
                db.entriesQueries.moveToProfile(destinationProfileId = 2, sourceProfileId = 1, entryId = 2)
            }
            expect("delete categories", "category", "category", "category.membership") {
                categories.delete(categories.getAll(1).single { it.name == "Before sync" }.id)
                categories.delete(categories.getAll(1).single { it.name == "Watching" }.id)
            }
            expect("source refresh") {
                entries.updateFromSourceSync(
                    entries.getEntryById(1, 1)!!.copy(title = "Show (TV)", description = "Refreshed"),
                    profileId = 1,
                    updateTitle = true,
                )
                val episode = chapters.getChapterById(10)!!
                // The source no longer lists the recap, whose history goes with it
                chapters.applySourceSync(
                    removedIds = listOf(12),
                    updated = listOf(episode.copy(name = "Episode 1: Pilot")),
                    inserted = listOf(
                        EntryChapter.create().copy(entryId = 1, url = "/show/2", name = "Episode 2", dateFetch = 1000),
                    ),
                    listedIds = listOf(10L),
                    afterUpdates = {},
                    afterInserts = {},
                )
            }
            expect("source refresh finding a new release", "chapter") {
                chapters.applySourceSync(
                    removedIds = emptyList(),
                    updated = emptyList(),
                    inserted = listOf(
                        EntryChapter.create().copy(entryId = 1, url = "/show/3", name = "Episode 3", dateFetch = 2000),
                    ),
                    listedIds = emptyList(),
                    afterUpdates = {},
                    afterInserts = {},
                )
            }
            expect("clear history", "history", "statistics.epoch") { history.deleteAllHistory() }
            // Rows that go with their entry, such as clearing the database of entries outside the library
            expect("entry removed as cache") { db.entriesQueries.deleteById(profileId = 1, entryId = 1) }

            val stored = device.handler.await {
                sync_recordsQueries.pendingAfter("", Long.MAX_VALUE).awaitAsList().map { it.kind }
            }
            // Preferences aren't in the database; their kinds have no triggers
            stored.toSet() shouldBe SyncKinds.all.filter { it.capture.isNotEmpty() }.map { it.name }.toSet()
            withClue("both episodes and the new release, not the local chapter") {
                stored.count { it == "chapter" } shouldBe 3
            }
        }
    }
}
