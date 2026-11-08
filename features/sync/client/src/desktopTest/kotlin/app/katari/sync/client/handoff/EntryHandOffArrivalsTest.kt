package app.katari.sync.client.handoff

import app.katari.sync.client.SyncingTestDevice
import app.katari.sync.client.TestSyncServer
import app.katari.sync.client.run.SyncOutcome
import app.katari.sync.client.showRows
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.Collections

class EntryHandOffArrivalsTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `progress recorded when a device set up sync is no other device's, and an edit after it is`(): Unit =
        runBlocking {
            // One page: a position is reported only once its entry is here, which a smaller page may bring later
            TestSyncServer(directory, pageSize = 500).use { server ->
                val account = server.account("reader")
                SyncingTestDevice(directory.resolve("a.db")).use { a ->
                    SyncingTestDevice(directory.resolve("b.db")).use { b ->
                        a.sql(
                            *showRows("/show", episodes = 2, read = 2),
                            """
                            INSERT INTO entry_progress_state(entry_id, chapter_id, resource_key, locator_kind, position)
                            SELECT entry_id, _id, url, 'time', 600 FROM chapters WHERE url = '/show/2'
                            """,
                        )
                        a.join(server, account)
                        a.sync() shouldBe SyncOutcome.SYNCED

                        val arrivals = Collections.synchronizedList(ArrayList<ProgressArrival>())
                        // Unconfined: each arrival is collected inside the emit, before the run that applied it returns
                        val listening = launch(Dispatchers.Unconfined, start = CoroutineStart.UNDISPATCHED) {
                            b.handOff.arrivals.collect { arrivals += it }
                        }
                        b.join(server, account)
                        b.sync() shouldBe SyncOutcome.SYNCED
                        arrivals.shouldBeEmpty()

                        a.sql("UPDATE entry_progress_state SET position = 900")
                        a.sync() shouldBe SyncOutcome.SYNCED
                        b.sync() shouldBe SyncOutcome.SYNCED
                        listening.cancel()
                        arrivals.map { it.entryId to it.deviceName } shouldBe listOf(b.local.entryId("/show") to "a")
                    }
                }
            }
        }
}
