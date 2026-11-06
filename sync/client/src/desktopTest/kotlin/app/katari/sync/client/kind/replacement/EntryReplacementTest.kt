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
                        """
                        INSERT INTO entry_sync(
                            entry_id, sync_id, remote_id, title, last_chapter_read, total_chapters, status, score,
                            remote_url, start_date, finish_date
                        )
                        VALUES (1, 2, 55, 'Show', 1, 12, 1, 0, 'https://anilist.co/anime/55', 0, 0)
                        """,
                    )
                    exchange.settle(listOf(a, b))
                    b.sql(
                        "INSERT INTO chapters(entry_id, url, name, chapter_number) SELECT _id, '/old/2', 'Episode 2', 2 FROM entries",
                    )

                    // A migrates the entry as local migration does, keeping chapter state, categories and notes: the
                    // new entry takes its place, its marks and its tracker binding, the old one leaves the library,
                    // and the position follows the matched episode
                    a.sql(
                        "INSERT INTO entries(_id, source, url, title, favorite, type) VALUES (2, -3, '/new', 'Show', 1, 'anime')",
                        """
                        INSERT INTO chapters(_id, entry_id, url, name, chapter_number, read)
                        VALUES (20, 2, '/new/1', 'Episode 1', 1, 1), (21, 2, '/new/2', 'Episode 2', 2, 0)
                        """,
                        "UPDATE entries SET favorite = 0 WHERE _id = 1",
                        """
                        INSERT INTO entry_sync(
                            entry_id, sync_id, remote_id, title, last_chapter_read, total_chapters, status, score,
                            remote_url, start_date, finish_date
                        )
                        SELECT 2, sync_id, remote_id, title, last_chapter_read, total_chapters, status, score,
                            remote_url, start_date, finish_date
                        FROM entry_sync WHERE entry_id = 1
                        """,
                        """
                        INSERT INTO entry_progress_state(entry_id, chapter_id, resource_key, locator_kind, position)
                        VALUES (2, 20, '/new/1', 'time', 600)
                        """,
                        """
                        INSERT INTO entry_migration_operations(
                            operation_id, intent_fingerprint, profile_id, source_entry_id, target_entry_id, mode,
                            created_at
                        )
                        VALUES ('operation', 'REPLACE:CATEGORIES,CHILD_STATE,NOTES', 1, 1, 2, 'REPLACE', 0)
                        """,
                    )
                    a.drainAll()
                    delay(5)
                    // B hasn't heard of it yet, watches the second episode of the old entry and updates the tracker
                    b.sql(
                        "UPDATE entries SET notes = 'Watched on the train'",
                        "UPDATE entry_sync SET status = 2, score = 8",
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
                        // Migrating leaves history with the old entry
                        """["history","/old/2",5000,1200]""",
                        """["progress","/new","","/new/1",600,0]""",
                        """["tracker","/new",2,55,2,8.0]""",
                    )
                }
            }
        }

    @Test
    fun `time read on an entry moved to another profile adds up with what was read on it before the move`(): Unit =
        runBlocking {
            val exchange = RecordExchange()
            SyncTestDevice(directory.resolve("a.db")).use { a ->
                SyncTestDevice(directory.resolve("b.db")).use { b ->
                    a.startSync(deviceId = 1)
                    b.startSync(deviceId = 2)
                    a.sql(
                        "INSERT INTO profiles(_id, uuid, name, color_seed, position) VALUES (2, 'work', 'Work', 1, 1)",
                        "INSERT INTO source_identities(local_key, id_scheme, external_id) VALUES (-2, 'aniyomi', 777)",
                        "INSERT INTO entries(_id, source, url, title, favorite, type) VALUES (1, -2, '/show', 'Show', 1, 'anime')",
                        "INSERT INTO chapters(_id, entry_id, url, name, chapter_number) VALUES (10, 1, '/show/1', 'Episode 1', 1)",
                        "INSERT INTO history(entry_id, chapter_id, last_read, time_read) VALUES (1, 10, 1000, 300)",
                    )
                    exchange.settle(listOf(a, b))
                    b.sql("UPDATE history SET time_read = 500")
                    exchange.settle(listOf(a, b))

                    // A moves the entry, and later watches more of the episode in the new profile
                    a.sql(
                        "UPDATE entries SET profile_id = 2 WHERE _id = 1",
                        "UPDATE history SET last_read = 2000, time_read = 600 WHERE chapter_id = 10",
                    )
                    a.drainAll()
                    delay(5)
                    // B hasn't heard of the move and watches the episode in the old profile
                    b.sql("UPDATE history SET last_read = 3000, time_read = 1700")

                    exchange.settle(listOf(a, b))

                    a.library() shouldBe b.library()
                    // 300 on A and 200 on B before the move, then 100 more on A and 1200 more on B
                    a.library() shouldContainAll listOf(
                        """["entry","work","aniyomi",777,"/show","anime",1,0,null,"",0,0,0]""",
                        """["history","/show/1",3000,1800]""",
                    )
                }
            }
        }
}
