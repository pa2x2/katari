package app.katari.sync.client.restore

import app.katari.sync.client.SyncingTestDevice
import app.katari.sync.client.TestSyncServer
import app.katari.sync.client.run.SyncOutcome
import app.katari.sync.client.run.SyncRequest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

class RestoreStampingTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `a restore fills in what is older than the backup and leaves what another device changed since`(): Unit =
        runBlocking {
            restoringOnA { a, backupCreatedAt ->
                a.restoreStamping.restoreOnThisDevice(backupCreatedAt) {
                    a.writeBackup()
                    // Writes ask for runs; one taking the restore's writes as edits made now would win over B's
                    a.runs.request(SyncRequest(onlyIfChanged = true))
                    withTimeoutOrNull(1.seconds) { a.runs.awaitIdle() } shouldBe null
                }
            }
        }

    @Test
    fun `a restore that fails partway records what it wrote as made when the backup was`(): Unit = runBlocking {
        restoringOnA { a, backupCreatedAt ->
            shouldThrow<IllegalStateException> {
                a.restoreStamping.restoreOnThisDevice(backupCreatedAt) {
                    a.writeBackup()
                    error("The backup ends early")
                }
            }
        }
    }

    @Test
    fun `a restore the app stopped in is recorded as made when the backup was once the app starts again`(): Unit =
        runBlocking {
            restoringOnA { a, backupCreatedAt ->
                // As the restore records itself when it starts, before the process ends in the middle of it
                a.local.handler.await { sync_stateQueries.startRestore(backupCreatedAt) }
                a.writeBackup()

                // The next process may run before it records the restore
                a.sync() shouldBe SyncOutcome.SYNCED
                a.restoreStamping.finishInterrupted()
            }
        }

    /**
     * Syncs A and B, changes both after a backup of A, runs [restore] on A and checks that only what is older than the
     * backup gave way to it.
     */
    private suspend fun restoringOnA(restore: suspend (a: SyncingTestDevice, backupCreatedAt: Long) -> Unit) {
        TestSyncServer(directory).use { server ->
            val account = server.account("reader")
            SyncingTestDevice(directory.resolve("a.db")).use { a ->
                SyncingTestDevice(directory.resolve("b.db")).use { b ->
                    a.sql(
                        "INSERT INTO source_identities(local_key, id_scheme, external_id) VALUES (-2, 'aniyomi', 777)",
                        """
                        INSERT INTO entries(_id, source, url, title, favorite, type, notes)
                        VALUES (1, -2, '/show', 'Show', 1, 'anime', 'Backed up')
                        """,
                        "INSERT INTO chapters(_id, entry_id, url, name, chapter_number) VALUES (10, 1, '/show/1', 'Ep 1', 1)",
                    )
                    a.join(server, account)
                    a.sync() shouldBe SyncOutcome.SYNCED
                    b.join(server, account)
                    b.sync() shouldBe SyncOutcome.SYNCED
                    a.displayMode().set("list")
                    a.sorting().set("ALPHABETICAL,ASCENDING")
                    a.sync() shouldBe SyncOutcome.SYNCED
                    b.sync() shouldBe SyncOutcome.SYNCED
                    delay(5)
                    val backupCreatedAt = Clock.System.now().toEpochMilliseconds()
                    delay(5)

                    // After the backup: B changes the notes and a setting, A marks the episode and a setting
                    b.sql("UPDATE entries SET notes = 'Changed since'")
                    b.displayMode().set("grid")
                    b.sync() shouldBe SyncOutcome.SYNCED
                    a.sql("UPDATE chapters SET read = 1")
                    a.sorting().set("LAST_READ,DESCENDING")
                    a.sync() shouldBe SyncOutcome.SYNCED
                    b.sync() shouldBe SyncOutcome.SYNCED

                    restore(a, backupCreatedAt)
                    a.sync() shouldBe SyncOutcome.SYNCED
                    b.sync() shouldBe SyncOutcome.SYNCED

                    a.library() shouldBe b.library()
                    a.library() shouldContainAll listOf(
                        """["entry","default","aniyomi",777,"/show","anime",1,0,null,"Changed since",0,0,0]""",
                        """["chapter","/show","/show/1",1,1]""",
                    )
                    listOf(a, b).forEach { device ->
                        device.displayMode().get() shouldBe "grid"
                        device.sorting().get() shouldBe "LAST_READ,DESCENDING"
                    }
                }
            }
        }
    }

    /** What the backup holds: the old notes and settings, the episode unread, and a bookmark nobody made since. */
    private suspend fun SyncingTestDevice.writeBackup() {
        sql("UPDATE entries SET notes = 'Backed up'", "UPDATE chapters SET read = 0, bookmark = 1")
        displayMode().set("list")
        sorting().set("ALPHABETICAL,ASCENDING")
    }

    private fun SyncingTestDevice.displayMode() =
        local.preferences.profiles.profileStore(1).getString("pref_display_mode_library")

    private fun SyncingTestDevice.sorting() = local.preferences.profiles.profileStore(
        1,
    ).getString("library_sorting_mode")
}
