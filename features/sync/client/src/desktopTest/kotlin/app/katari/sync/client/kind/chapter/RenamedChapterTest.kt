package app.katari.sync.client.kind.chapter

import app.katari.sync.client.RecordExchange
import app.katari.sync.client.SyncTestDevice
import app.katari.sync.client.library
import io.kotest.matchers.shouldBe
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
}
