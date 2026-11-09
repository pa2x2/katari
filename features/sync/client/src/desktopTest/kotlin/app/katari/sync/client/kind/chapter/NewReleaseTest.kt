package app.katari.sync.client.kind.chapter

import app.katari.sync.client.RecordExchange
import app.katari.sync.client.SyncTestDevice
import app.katari.sync.client.rows
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class NewReleaseTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `a release one device's refresh found is in the other's Updates and newest in its list`(): Unit = runBlocking {
        val exchange = RecordExchange()
        SyncTestDevice(directory.resolve("a.db")).use { a ->
            SyncTestDevice(directory.resolve("b.db")).use { b ->
                a.startSync(deviceId = 1)
                b.startSync(deviceId = 2)
                // The entry joined the Library at 1000, after A listed its two chapters and read the first
                a.sql(
                    "INSERT INTO source_identities(local_key, id_scheme, external_id) VALUES (-2, 'aniyomi', 777)",
                    """
                    INSERT INTO entries(_id, source, url, title, favorite, date_added, type)
                    VALUES (1, -2, '/show', 'Show', 1, 1000, 'anime')
                    """,
                    """
                    INSERT INTO chapters(entry_id, url, name, read, source_order, date_fetch)
                    VALUES (1, '/show/1', 'Episode 1', 1, 1, 500), (1, '/show/2', 'Episode 2', 0, 0, 500)
                    """,
                )
                exchange.deliver(b, exchange.pushFrom(a))
                // B's own refresh lists the chapter nobody marked before the one read, which sync put in A's place
                b.sql(
                    """
                    INSERT INTO chapters(entry_id, url, name, source_order, date_fetch)
                    SELECT _id, '/show/2', 'Episode 2', 0, 800 FROM entries
                    """,
                )

                // A's library update finds episode 3
                a.sql(
                    "UPDATE chapters SET source_order = source_order + 1",
                    "INSERT INTO chapters(entry_id, url, name, source_order, date_fetch) VALUES (1, '/show/3', 'Episode 3', 0, 2000)",
                )
                exchange.deliver(b, exchange.pushFrom(a))

                b.rows("SELECT json_array(url, source_order, date_fetch) FROM chapters ORDER BY source_order") shouldBe
                    listOf("""["/show/3",0,2000]""", """["/show/2",1,800]""", """["/show/1",2,500]""")
                b.rows("SELECT chapterUrl FROM updatesView") shouldBe listOf("/show/3")
                b.rows("SELECT json_array(last_update) FROM entries") shouldBe listOf("[2000]")
            }
        }
    }

    @Test
    fun `a chapter one device found as a release and another as back catalogue is a release on both`(): Unit =
        runBlocking {
            val exchange = RecordExchange()
            SyncTestDevice(directory.resolve("a.db")).use { a ->
                SyncTestDevice(directory.resolve("b.db")).use { b ->
                    a.startSync(deviceId = 1)
                    b.startSync(deviceId = 2)
                    a.sql(
                        "INSERT INTO source_identities(local_key, id_scheme, external_id) VALUES (-2, 'aniyomi', 777)",
                        """
                        INSERT INTO entries(_id, source, url, title, favorite, date_added, type)
                        VALUES (1, -2, '/show', 'Show', 1, 1000, 'anime')
                        """,
                    )
                    exchange.settle(listOf(a, b))
                    // B lists the entry for the first time, dating it as back catalogue, and reads it before it hears
                    // of A's library update finding it
                    b.sql(
                        """
                        INSERT INTO chapters(entry_id, url, name, source_order, date_fetch)
                        SELECT _id, '/show/1', 'Episode 1', 0, 1000 FROM entries
                        """,
                        "UPDATE chapters SET read = 1",
                    )
                    a.sql(
                        "INSERT INTO chapters(entry_id, url, name, source_order, date_fetch) VALUES (1, '/show/1', 'Episode 1', 0, 2000)",
                    )

                    exchange.settle(listOf(a, b))

                    listOf(a, b).forEach { device ->
                        device.rows("SELECT json_array(chapterUrl, read, datefetch) FROM updatesView") shouldBe
                            listOf("""["/show/1",1,2000]""")
                    }
                }
            }
        }
}
