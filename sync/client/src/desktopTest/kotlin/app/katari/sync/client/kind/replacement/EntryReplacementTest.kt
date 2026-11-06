package app.katari.sync.client.kind.replacement

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

class EntryReplacementTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `a migration moves the entry on the other device, with what was done to the old entry after it`(): Unit =
        runBlocking {
            val exchange = RecordExchange()
            SyncTestDevice(directory.resolve("a.db")).use { a ->
                SyncTestDevice(directory.resolve("b.db")).use { b ->
                    a.startSync(deviceId = 1)
                    b.startSync(deviceId = 2)
                    a.sql(
                        """
                        INSERT INTO source_identities(local_key, id_scheme, external_id)
                        VALUES (-2, 'aniyomi', 777), (-3, 'aniyomi', 888)
                        """,
                        "INSERT INTO entries(_id, source, url, title, favorite, type) VALUES (1, -2, '/old', 'Show', 1, 'anime')",
                        """
                        INSERT INTO chapters(_id, entry_id, url, name, chapter_number, read)
                        VALUES (10, 1, '/old/1', 'Episode 1', 1, 1), (11, 1, '/old/2', 'Episode 2', 2, 0)
                        """,
                    )
                    exchange.settle(listOf(a, b))
                    b.sql(
                        "INSERT INTO chapters(entry_id, url, name, chapter_number) SELECT _id, '/old/2', 'Episode 2', 2 FROM entries",
                    )

                    // A migrates the entry as local migration does: the new entry takes its place and its marks, the
                    // old one leaves the library, and the position follows the matched episode
                    a.sql(
                        "INSERT INTO entries(_id, source, url, title, favorite, type) VALUES (2, -3, '/new', 'Show', 1, 'anime')",
                        """
                        INSERT INTO chapters(_id, entry_id, url, name, chapter_number, read)
                        VALUES (20, 2, '/new/1', 'Episode 1', 1, 1), (21, 2, '/new/2', 'Episode 2', 2, 0)
                        """,
                        "UPDATE entries SET favorite = 0 WHERE _id = 1",
                        """
                        INSERT INTO entry_progress_state(entry_id, chapter_id, resource_key, locator_kind, position)
                        VALUES (2, 20, '/new/1', 'time', 600)
                        """,
                        """
                        INSERT INTO entry_migration_operations(
                            operation_id, intent_fingerprint, profile_id, source_entry_id, target_entry_id, mode,
                            created_at
                        )
                        VALUES ('operation', 'fingerprint', 1, 1, 2, 'REPLACE', 0)
                        """,
                    )
                    a.drainAll()
                    delay(5)
                    // B hasn't heard of it yet and watches the second episode of the old entry
                    b.sql(
                        "UPDATE entries SET notes = 'Watched on the train'",
                        "UPDATE chapters SET read = 1 WHERE url = '/old/2'",
                        """
                        INSERT INTO history(entry_id, chapter_id, last_read, time_read)
                        SELECT entry_id, _id, 5000, 1200 FROM chapters WHERE url = '/old/2'
                        """,
                    )

                    exchange.settle(listOf(a, b))

                    a.library() shouldBe b.library()
                    a.library() shouldContainAll listOf(
                        """["entry","default","aniyomi",777,"/old","anime",0,0,null,"Watched on the train",0,0,0]""",
                        """["entry","default","aniyomi",888,"/new","anime",1,0,null,"Watched on the train",0,0,0]""",
                        """["chapter","/new","/new/1",1,0]""",
                        """["chapter","/new","/new/2",1,0]""",
                        """["history","/new/2",5000,1200]""",
                        """["progress","/new","","/new/1",600,0]""",
                    )
                }
            }
        }
}
