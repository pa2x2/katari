package app.katari.sync.client.apply

import app.katari.sync.client.RecordExchange
import app.katari.sync.client.SyncTestDevice
import app.katari.sync.client.kind.SyncKinds
import app.katari.sync.client.library
import app.katari.sync.client.rows
import app.katari.sync.protocol.record.SequencedRecord
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ConvergenceTest {

    @TempDir
    lateinit var directory: File

    private val deliveries: Map<String, Pair<Boolean, (List<SequencedRecord>) -> List<SequencedRecord>>> = mapOf(
        "as pushed, in one page" to (false to { it }),
        "as pushed, one record per page" to (true to { it }),
        "reversed, one record per page" to (true to { it.reversed() }),
        "children first, one record per page" to (true to { records -> records.sortedByDescending(::kindOrder) }),
    )

    @Test
    fun `offline edits on two devices converge in every delivery order`(): Unit = runBlocking {
        deliveries.forEach { (name, delivery) ->
            val (onePerPage, order) = delivery
            withClue(name) { offlineEditsConverge(directory.resolve(name).apply { mkdirs() }, onePerPage, order) }
        }
    }

    private suspend fun offlineEditsConverge(
        directory: File,
        onePerPage: Boolean,
        order: (List<SequencedRecord>) -> List<SequencedRecord>,
    ) {
        val exchange = RecordExchange()
        SyncTestDevice(directory.resolve("a.db")).use { a ->
            SyncTestDevice(directory.resolve("b.db")).use { b ->
                a.startSync(deviceId = 1)
                b.startSync(deviceId = 2)
                a.sql(
                    "INSERT INTO source_identities(local_key, id_scheme, external_id) VALUES (-2, 'aniyomi', 777)",
                    "INSERT INTO entries(_id, source, url, title, favorite, type) VALUES (1, -2, '/show', 'Show', 1, 'anime')",
                    """
                    INSERT INTO chapters(_id, entry_id, url, name, chapter_number, read)
                    VALUES (10, 1, '/show/1', 'Episode 1', 1, 1), (11, 1, '/show/2', 'Episode 2', 2, 0),
                        (12, 1, '/show/3', 'Episode 3', 3, 0)
                    """,
                    "INSERT INTO history(entry_id, chapter_id, last_read, time_read) VALUES (1, 10, 1000, 1000)",
                    """
                    INSERT INTO entry_sync(entry_id, sync_id, remote_id, title, last_chapter_read, total_chapters,
                        status, score, remote_url, start_date, finish_date)
                    VALUES (1, 2, 99, 'Show', 1, 12, 1, 7, 'https://tracker/99', 0, 0)
                    """,
                )
                exchange.deliver(b, exchange.pushFrom(a))
                withClue("applying produces nothing to push") {
                    b.pending.count() shouldBe 0
                    b.outboxKinds().shouldBeEmpty()
                }
                // B's refresh of the entry brings the chapters nobody marked, which is no change
                b.sql(
                    """
                    INSERT INTO chapters(entry_id, url, name, chapter_number)
                    SELECT _id, '/show/2', 'Episode 2', 2 FROM entries
                    UNION ALL SELECT _id, '/show/3', 'Episode 3', 3 FROM entries
                    """,
                )
                b.outboxKinds().shouldBeEmpty()

                b.sql("UPDATE entry_sync SET score = 9")
                b.drainAll()
                delay(5)
                a.sql(
                    "UPDATE entries SET notes = 'A notes'",
                    "UPDATE chapters SET read = 0 WHERE url = '/show/1'",
                    "UPDATE chapters SET read = 1 WHERE url = '/show/2'",
                    "DELETE FROM history",
                    "DELETE FROM entry_sync",
                )
                a.drainAll()
                delay(5)
                b.sql(
                    "UPDATE entries SET notes = 'B notes', display_name = 'B name'",
                    "UPDATE chapters SET bookmark = 1 WHERE url = '/show/3'",
                    "UPDATE history SET time_read = time_read + 500, last_read = 3000",
                )

                exchange.settle(listOf(a, b), onePerPage, order)

                a.library() shouldBe b.library()
                a.library() shouldContainAll listOf(
                    """["entry","default","aniyomi",777,"/show","anime",1,0,"B name","B notes",0,0,0]""",
                    // Marked unread on A, which nobody overrode
                    """["chapter","/show","/show/1",0,0]""",
                    """["chapter","/show","/show/2",1,0]""",
                    """["chapter","/show","/show/3",0,1]""",
                    // B read on after A removed it from history, so it is back, with both devices' time
                    """["history","/show/1",3000,1500]""",
                )
                withClue("A unbound the tracker after B's edit of it") {
                    a.rows("SELECT json_array(sync_id) FROM entry_sync").shouldBeEmpty()
                    b.rows("SELECT json_array(sync_id) FROM entry_sync").shouldBeEmpty()
                }
            }
        }
    }

    private fun kindOrder(record: SequencedRecord): Int = SyncKinds.all.indexOfFirst { it.name == record.record.kind }
}
