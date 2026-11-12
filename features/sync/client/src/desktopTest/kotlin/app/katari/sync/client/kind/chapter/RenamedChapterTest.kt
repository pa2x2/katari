package app.katari.sync.client.kind.chapter

import app.katari.sync.client.RecordExchange
import app.katari.sync.client.SyncTestDevice
import app.katari.sync.client.library
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class RenamedChapterTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `a chapter one device's refresh found at a new URL moves there on the other, with its marks and history`():
        Unit = runBlocking {
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
                    VALUES (10, 1, '/show/1', 'Episode 1', 1, 1), (11, 1, '/show/2', 'Episode 2', 2, 0)
                    """,
                    "INSERT INTO history(entry_id, chapter_id, last_read, time_read) VALUES (1, 10, 1000, 600)",
                )
                exchange.settle(listOf(a, b))
                b.sql(
                    "INSERT INTO chapters(entry_id, url, name, chapter_number) SELECT _id, '/show/2', 'Episode 2', 2 FROM entries",
                )

                // The source moved its episodes, which A's refresh matched to the rows it had
                a.sql(
                    "UPDATE chapters SET url = '/show/e1' WHERE _id = 10",
                    "UPDATE chapters SET url = '/show/e2' WHERE _id = 11",
                )
                exchange.settle(listOf(a, b))

                // B's own refresh moves the episode nobody marked, as A's did
                b.library().filter { it.startsWith("""["chapter"""") || it.startsWith("""["history"""") } shouldBe
                    listOf(
                        """["chapter","/show","/show/2",0,0]""",
                        """["chapter","/show","/show/e1",1,0]""",
                        """["history","/show/e1",1000,600]""",
                    )
            }
        }
    }

    @Test
    fun `a position read at a chapter's old URL before another device's refresh moved it is kept at the new URL`() =
        editsAtOldUrlWhileMoved(readAt = { 2000 })

    @Test
    fun `marks and a position made at a chapter's old URL after another device's refresh moved it go to the new URL`() =
        editsAtOldUrlWhileMoved(readAt = System::currentTimeMillis)

    /**
     * A's refresh moves an episode and its position to a new URL; B, not having heard of it, bookmarks the episode
     * and reads on at the old URL, [readAt]. The records cross: A hears of B's edits before B hears of the move.
     */
    private fun editsAtOldUrlWhileMoved(readAt: () -> Long): Unit = runBlocking {
        val exchange = RecordExchange()
        SyncTestDevice(directory.resolve("a.db")).use { a ->
            SyncTestDevice(directory.resolve("b.db")).use { b ->
                a.startSync(deviceId = 1)
                b.startSync(deviceId = 2)
                a.sql(
                    "INSERT INTO source_identities(local_key, id_scheme, external_id) VALUES (-2, 'aniyomi', 777)",
                    "INSERT INTO entries(_id, source, url, title, favorite, type) VALUES (1, -2, '/show', 'Show', 1, 'anime')",
                    "INSERT INTO chapters(_id, entry_id, url, name, chapter_number) VALUES (10, 1, '/show/1', 'Episode 1', 1)",
                    """
                    INSERT INTO entry_progress_state(entry_id, chapter_id, resource_key, locator_kind, position, locator_updated_at)
                    VALUES (1, 10, '/show/1', 'video', 10, 1000)
                    """,
                )
                exchange.settle(listOf(a, b))

                a.sql(
                    "UPDATE chapters SET url = '/show/e1' WHERE _id = 10",
                    "UPDATE entry_progress_state SET resource_key = '/show/e1'",
                )
                val moved = exchange.pushFrom(a)
                delay(5)
                b.sql(
                    "UPDATE chapters SET bookmark = 1",
                    "UPDATE entry_progress_state SET position = 900, locator_updated_at = ${readAt()}",
                )
                exchange.deliver(a, exchange.pushFrom(b))
                exchange.deliver(b, moved)
                exchange.settle(listOf(a, b))

                b.library().filter { it.startsWith("""["chapter"""") || it.startsWith("""["progress"""") } shouldBe
                    listOf(
                        """["chapter","/show","/show/e1",0,1]""",
                        """["progress","/show","","/show/e1",900,0]""",
                    )
                a.library() shouldBe b.library()
            }
        }
    }
}
