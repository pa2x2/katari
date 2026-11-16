package app.katari.sync.client.kind.replacement

import app.katari.sync.client.RecordExchange
import app.katari.sync.client.SyncTestDevice
import app.katari.sync.client.library
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ReplacedEntryReturnTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `an entry added back after migrating away from it is back in the library on the other device`(): Unit =
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
                        """
                        INSERT INTO entries(_id, source, url, title, favorite, type, date_added)
                        VALUES (1, -2, '/old', 'Show', 1, 'anime', 100)
                        """,
                        """
                        INSERT INTO activity_sessions(session_id, entry_id, started_at, ended_at, duration)
                        VALUES ('before', 1, 1000, 1500, 500)
                        """,
                    )
                    exchange.settle(listOf(a, b))

                    // As the app migrates: the operation first, then the new entry takes the old one's place and
                    // activity, and the old one leaves the library
                    a.sql(
                        "INSERT INTO entries(_id, source, url, title, favorite, type) VALUES (2, -3, '/new', 'Show', 0, 'anime')",
                        """
                        INSERT INTO entry_migration_operations(
                            operation_id, intent_fingerprint, profile_id, source_entry_id, target_entry_id, mode,
                            created_at
                        )
                        VALUES ('operation', 'REPLACE:', 1, 1, 2, 'REPLACE', 0)
                        """,
                        "UPDATE entries SET favorite = 1, date_added = 100 WHERE _id = 2",
                        "UPDATE entries SET favorite = 0, library_pinned = 0, date_added = 0 WHERE _id = 1",
                        "UPDATE activity_sessions SET entry_id = 2",
                    )
                    exchange.settle(listOf(a, b))

                    // Later the person adds the old entry back from its source and watches it
                    val later = System.currentTimeMillis() + 60_000
                    a.sql(
                        "UPDATE entries SET favorite = 1, date_added = 9000 WHERE _id = 1",
                        """
                        INSERT INTO activity_sessions(session_id, entry_id, started_at, ended_at, duration)
                        VALUES ('after', 1, $later, ${later + 500}, 500)
                        """,
                    )
                    exchange.settle(listOf(a, b))

                    a.library() shouldBe b.library()
                    b.library() shouldContainAll listOf(
                        """["entry","default","aniyomi",777,"/old","anime",1,9000,null,"",0,0,0]""",
                        // Adding the old entry back is no edit of the new one
                        """["entry","default","aniyomi",888,"/new","anime",1,100,null,"",0,0,0]""",
                        """["session","before","/new",1000,1500,500]""",
                        """["session","after","/old",$later,${later + 500},500]""",
                    )
                }
            }
        }

    @Test
    fun `an entry moved to another profile and back is in its first profile on the other device`(): Unit =
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
                        """
                        INSERT INTO chapters(_id, entry_id, url, name, chapter_number, read)
                        VALUES (10, 1, '/show/1', 'Episode 1', 1, 1)
                        """,
                    )
                    exchange.settle(listOf(a, b))

                    // Twice: the second move away is to where the first went, after the entry came back from there
                    repeat(2) {
                        a.sql("UPDATE entries SET profile_id = 2 WHERE _id = 1")
                        exchange.settle(listOf(a, b))
                        b.library() shouldContain """["entry","work","aniyomi",777,"/show","anime",1,0,null,"",0,0,0]"""
                        a.sql("UPDATE entries SET profile_id = 1 WHERE _id = 1")
                        // The move back can arrive before the entry's record that says it came back
                        exchange.settle(listOf(a, b), onePerPage = true) { records ->
                            records.sortedBy { it.record.kind != EntryReplacementKind.name }
                        }

                        a.library() shouldBe b.library()
                        b.library() shouldContainAll listOf(
                            """["entry","default","aniyomi",777,"/show","anime",1,0,null,"",0,0,0]""",
                            """["chapter","/show","/show/1",1,0]""",
                        )
                    }
                }
            }
        }
}
