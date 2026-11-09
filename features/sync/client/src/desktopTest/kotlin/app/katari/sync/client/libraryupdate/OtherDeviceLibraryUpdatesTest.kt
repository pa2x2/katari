package app.katari.sync.client.libraryupdate

import app.katari.sync.client.SyncingTestDevice
import app.katari.sync.client.TestSyncServer
import app.katari.sync.client.run.SyncOutcome
import app.katari.sync.client.showRows
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class OtherDeviceLibraryUpdatesTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `the newest library update reaches every device, whichever device updates again`(): Unit = runBlocking {
        TestSyncServer(directory, pageSize = 500).use { server ->
            val account = server.account("reader")
            SyncingTestDevice(directory.resolve("a.db")).use { a ->
                SyncingTestDevice(directory.resolve("b.db")).use { b ->
                    a.sql(*showRows("/show"))
                    a.join(server, account)
                    a.sync() shouldBe SyncOutcome.SYNCED
                    b.join(server, account)
                    b.sync() shouldBe SyncOutcome.SYNCED

                    a.sql(
                        run(startedAt = 1000, finishedAt = 2000),
                        """
                        INSERT INTO entry_update_status(entry_id, decided_at, outcome, error)
                        SELECT _id, 1500, 'failed', 'Timeout' FROM entries WHERE url = '/show'
                        """,
                    )
                    a.sync() shouldBe SyncOutcome.SYNCED
                    b.sync() shouldBe SyncOutcome.SYNCED
                    a.otherUpdate().shouldBeNull()
                    b.otherUpdate().shouldNotBeNull().run {
                        deviceName shouldBe "a"
                        summary.run.finishedAt shouldBe 2000
                        summary.failed shouldBe 1
                    }

                    // An update b made before a's, read only now, leaves a's in place on both devices
                    b.sql(run(startedAt = 500, finishedAt = 600))
                    b.sync() shouldBe SyncOutcome.SYNCED
                    a.sync() shouldBe SyncOutcome.SYNCED
                    a.otherUpdate().shouldBeNull()
                    b.otherUpdate().shouldNotBeNull().summary.run.startedAt shouldBe 1000

                    b.sql(run(startedAt = 3000, finishedAt = 4000))
                    b.sync() shouldBe SyncOutcome.SYNCED
                    a.sync() shouldBe SyncOutcome.SYNCED
                    b.otherUpdate().shouldBeNull()
                    a.otherUpdate().shouldNotBeNull().run {
                        deviceName shouldBe "b"
                        summary.run.startedAt shouldBe 3000
                        summary.failed shouldBe 0
                    }
                }
            }
        }
    }

    private fun run(startedAt: Long, finishedAt: Long) =
        """
        INSERT INTO library_update_runs(profile_id, started_at, finished_at, trigger, library_size)
        VALUES (1, $startedAt, $finishedAt, 'automatic', 1)
        ON CONFLICT(profile_id) DO UPDATE SET
            started_at = excluded.started_at,
            finished_at = excluded.finished_at
        """

    private suspend fun SyncingTestDevice.otherUpdate() = otherDeviceLibraryUpdates.subscribe(profileId = 1).first()
}
