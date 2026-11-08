package app.katari.sync.client.restore

import app.katari.sync.client.RecordExchange
import app.katari.sync.client.SyncTestDevice
import app.katari.sync.client.library
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.time.Clock

class RestoreStampingTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `a restore fills in what is older than the backup and leaves what another device changed since`(): Unit =
        runBlocking {
            val exchange = RecordExchange()
            SyncTestDevice(directory.resolve("a.db")).use { a ->
                SyncTestDevice(directory.resolve("b.db")).use { b ->
                    a.startSync(deviceId = 1)
                    b.startSync(deviceId = 2)
                    a.sql(
                        "INSERT INTO source_identities(local_key, id_scheme, external_id) VALUES (-2, 'aniyomi', 777)",
                        """
                        INSERT INTO entries(_id, source, url, title, favorite, type, notes)
                        VALUES (1, -2, '/show', 'Show', 1, 'anime', 'Backed up')
                        """,
                        "INSERT INTO chapters(_id, entry_id, url, name, chapter_number) VALUES (10, 1, '/show/1', 'Ep 1', 1)",
                    )
                    a.preferences.profiles.profileStore(1).getString(DISPLAY_MODE).set("list")
                    a.preferences.profiles.profileStore(1).getString(SORTING).set("ALPHABETICAL,ASCENDING")
                    exchange.settle(listOf(a, b))
                    delay(5)
                    val backupCreatedAt = Clock.System.now().toEpochMilliseconds()
                    delay(5)

                    // After the backup: B changes the notes and a setting, A marks the episode and a setting
                    b.sql("UPDATE entries SET notes = 'Changed since'")
                    b.preferences.profiles.profileStore(1).getString(DISPLAY_MODE).set("grid")
                    a.sql("UPDATE chapters SET read = 1")
                    a.preferences.profiles.profileStore(1).getString(SORTING).set("LAST_READ,DESCENDING")
                    exchange.settle(listOf(a, b))

                    // The backup has the old notes and settings, the episode unread, and a bookmark nobody made since
                    a.restoreStamping.restoreOnThisDevice(backupCreatedAt) {
                        a.sql("UPDATE entries SET notes = 'Backed up'", "UPDATE chapters SET read = 0, bookmark = 1")
                        a.preferences.profiles.profileStore(1).getString(DISPLAY_MODE).set("list")
                        a.preferences.profiles.profileStore(1).getString(SORTING).set("ALPHABETICAL,ASCENDING")
                    }
                    exchange.settle(listOf(a, b))

                    a.library() shouldBe b.library()
                    a.library() shouldContainAll listOf(
                        """["entry","default","aniyomi",777,"/show","anime",1,0,null,"Changed since",0,0,0]""",
                        """["chapter","/show","/show/1",1,1]""",
                    )
                    listOf(a, b).forEach { device ->
                        device.preferences.profiles.profileStore(1).getString(DISPLAY_MODE).get() shouldBe "grid"
                        device.preferences.profiles.profileStore(1).getString(SORTING).get() shouldBe
                            "LAST_READ,DESCENDING"
                    }
                }
            }
        }

    private companion object {
        const val DISPLAY_MODE = "pref_display_mode_library"
        const val SORTING = "library_sorting_mode"
    }
}
