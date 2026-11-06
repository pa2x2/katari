package app.katari.sync.client.setting.capture

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.katari.sync.client.SyncTestDevice
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class SettingsCaptureTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `with sync on, only edits of person-level preferences and source settings reach the outbox`(): Unit =
        runBlocking {
            SyncTestDevice(directory.resolve("device.db")).use { device ->
                val profiles = device.preferences.profiles
                val sourceFiles = device.preferences.sourceFiles
                // A source's settings from before profiles owned them, which its first open moves into every profile
                device.preferences.legacySourceFiles.open("source_777").edit().putString("server", "a").commit()

                profiles.profileStore(1).getString("pref_display_mode_library").set("list")
                device.outboxEdits().shouldBeEmpty()

                device.startSync(deviceId = 1)
                // Device-level, in a profile and in the shared store
                profiles.profileStore(1).getString("pref_library_cover_size").set("Large")
                profiles.basePreferenceStore().getString("verbose_logging").set("true")
                // Restore's copy of the default profile's preferences into the shared store
                profiles.basePreferenceStore().getString("pref_display_mode_library").set("list")
                // The move of the source's old settings: a migration
                sourceFiles.open(1, 777).getString("server", null) shouldBe "a"
                // Edits
                profiles.profileStore(1).getString("pref_display_mode_library").set("grid")
                profiles.basePreferenceStore().getBoolean("mark_duplicate_read_chapter_read").set(true)
                sourceFiles.open(1, 777).edit().putString("quality", "high").commit()

                val expected = listOf(
                    "setting.profile" to """[1,"pref_display_mode_library"]""",
                    "setting.shared" to """["mark_duplicate_read_chapter_read"]""",
                    "setting.source" to """[1,777,"quality"]""",
                )
                device.awaitOutboxEdits(expected.size) shouldBe expected
            }
        }

    private suspend fun SyncTestDevice.outboxEdits(): List<Pair<String, String>> = handler.await {
        sync_outboxQueries.batch(Long.MAX_VALUE).awaitAsList().map { it.kind to it.local_ref }
    }

    // Edits are recorded in the background, in the order they were made
    private suspend fun SyncTestDevice.awaitOutboxEdits(count: Int): List<Pair<String, String>> = withTimeout(5_000) {
        var edits = outboxEdits()
        while (edits.size < count) {
            delay(10)
            edits = outboxEdits()
        }
        edits
    }
}
