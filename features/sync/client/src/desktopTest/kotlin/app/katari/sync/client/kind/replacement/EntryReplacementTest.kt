package app.katari.sync.client.kind.replacement

import app.katari.sync.client.RecordExchange
import app.katari.sync.client.SyncTestDevice
import app.katari.sync.client.kind.chapter.ChapterGroup
import app.katari.sync.client.kind.chapter.ChapterKind
import app.katari.sync.client.library
import app.katari.sync.client.rows
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
                        INSERT INTO viewer_setting_overrides(entry_id, provider_id, setting_key, encoded_value, updated_at)
                        VALUES (1, 'player', 'speed', '1.0', 0)
                        """,
                    )
                    exchange.settle(listOf(a, b))
                    b.sql(
                        "INSERT INTO chapters(entry_id, url, name, chapter_number) SELECT _id, '/old/2', 'Episode 2', 2 FROM entries",
                    )

                    // A migrates the entry as local migration does, keeping chapter state, categories and notes: the
                    // new entry takes its place, its marks and its viewer setting, the old one leaves the library,
                    // and the position follows the matched episode
                    a.sql(
                        "INSERT INTO entries(_id, source, url, title, favorite, type) VALUES (2, -3, '/new', 'Show', 1, 'anime')",
                        """
                        INSERT INTO chapters(_id, entry_id, url, name, chapter_number, read)
                        VALUES (20, 2, '/new/1', 'Episode 1', 1, 1), (21, 2, '/new/2', 'Episode 2', 2, 0)
                        """,
                        "UPDATE entries SET favorite = 0 WHERE _id = 1",
                        """
                        INSERT INTO viewer_setting_overrides(entry_id, provider_id, setting_key, encoded_value, updated_at)
                        SELECT 2, provider_id, setting_key, encoded_value, updated_at
                        FROM viewer_setting_overrides WHERE entry_id = 1
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
                    // B hasn't heard of it yet, watches the second episode of the old entry and changes its speed
                    b.sql(
                        "UPDATE entries SET notes = 'Watched on the train'",
                        "UPDATE viewer_setting_overrides SET encoded_value = '1.5'",
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
                        // Time read on the old entry after the migration stays with it
                        """["history","/old/2",5000,1200]""",
                        """["progress","/new","","/new/1",600,0]""",
                        """["viewer setting","/new","player","speed","1.5"]""",
                    )
                }
            }
        }

    @Test
    fun `a mark carried to a chapter nobody marked yet arrives with what its source listed`(): Unit = runBlocking {
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

                a.sql(
                    "INSERT INTO entries(_id, source, url, title, favorite, type) VALUES (2, -3, '/new', 'Show', 1, 'anime')",
                    """
                    INSERT INTO chapters(_id, entry_id, url, name, chapter_number, read, scanlator, date_upload, memo)
                    VALUES
                        (20, 2, '/new/1', 'Episode 1', 1, 1, NULL, 0, CAST('{}' AS BLOB)),
                        (21, 2, '/new/2', 'Episode 2', 2, 0, 'Fansub', 3000, CAST('{"hash":"9f3c"}' AS BLOB))
                    """,
                    "UPDATE entries SET favorite = 0 WHERE _id = 1",
                    """
                    INSERT INTO entry_migration_operations(
                        operation_id, intent_fingerprint, profile_id, source_entry_id, target_entry_id, mode, created_at
                    )
                    VALUES ('operation', 'REPLACE:CHILD_STATE', 1, 1, 2, 'REPLACE', 0)
                    """,
                )
                a.drainAll()
                delay(5)
                // B hasn't heard of the migration yet, and watches the second episode, which A carries over
                b.sql("UPDATE chapters SET read = 1 WHERE url = '/old/2'")
                exchange.settle(listOf(a, b))

                b.rows(
                    """
                    SELECT json_array(C.read, C.scanlator, C.date_upload, CAST(C.memo AS TEXT))
                    FROM chapters C WHERE C.url = '/new/2'
                    """,
                ) shouldBe listOf("""[1,"Fansub",3000,"{\"hash\":\"9f3c\"}"]""")
            }
        }
    }

    @Test
    fun `a migration takes the time read along on every device, also a session from before it was heard of`(): Unit =
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
                        "INSERT INTO entries(_id, source, url, title, favorite, type) VALUES (1, -2, '/old', 'Book', 1, 'book')",
                        "INSERT INTO chapters(_id, entry_id, url, name, chapter_number) VALUES (10, 1, '/old/1', 'Book', 1)",
                        "INSERT INTO history(entry_id, chapter_id, last_read, time_read) VALUES (1, 10, 1000, 300)",
                        """
                        INSERT INTO activity_sessions(session_id, entry_id, started_at, ended_at, duration)
                        VALUES ('session-a', 1, 700, 1000, 300)
                        """,
                    )
                    exchange.settle(listOf(a, b))
                    b.sql(
                        "UPDATE history SET last_read = 2000, time_read = 500",
                        """
                        INSERT INTO activity_sessions(session_id, entry_id, started_at, ended_at, duration)
                        SELECT 'session-b', _id, 1800, 2000, 200 FROM entries
                        """,
                    )
                    exchange.settle(listOf(a, b))

                    // A migrates the entry in replace mode as local migration does: the old chapter's history moves to
                    // the matched new chapter, and the old entry's sessions to the new entry
                    a.sql(
                        "INSERT INTO entries(_id, source, url, title, favorite, type) VALUES (2, -3, '/new', 'Book', 1, 'book')",
                        "INSERT INTO chapters(_id, entry_id, url, name, chapter_number) VALUES (20, 2, '/new/1', 'Book', 1)",
                        """
                        INSERT INTO entry_migration_operations(
                            operation_id, intent_fingerprint, profile_id, source_entry_id, target_entry_id, mode,
                            created_at
                        )
                        VALUES ('operation', 'REPLACE:CHILD_STATE', 1, 1, 2, 'REPLACE', 0)
                        """,
                        "UPDATE entries SET favorite = 0 WHERE _id = 1",
                        """
                        INSERT INTO history(entry_id, chapter_id, last_read, time_read)
                        SELECT 2, 20, last_read, time_read FROM history WHERE chapter_id = 10
                        """,
                        "DELETE FROM history WHERE chapter_id = 10",
                        "UPDATE activity_sessions SET entry_id = 2 WHERE entry_id = 1",
                    )
                    a.drainAll()
                    delay(5)
                    // B hasn't heard of it yet and reads the old entry once more
                    b.sql(
                        """
                        INSERT INTO activity_sessions(session_id, entry_id, started_at, ended_at, duration)
                        SELECT 'session-late', _id, 3000, 3100, 100 FROM entries WHERE url = '/old'
                        """,
                    )

                    exchange.settle(listOf(a, b))

                    a.library() shouldBe b.library()
                    a.library().filter { it.startsWith("""["history"""") || it.startsWith("""["session"""") } shouldBe
                        listOf(
                            """["history","/new/1",2000,500]""",
                            """["session","session-a","/new",700,1000,300]""",
                            """["session","session-b","/new",1800,2000,200]""",
                            """["session","session-late","/new",3000,3100,100]""",
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

    @Test
    fun `a profile move takes the entry's reading positions along to the entry in the other profile`(): Unit =
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
                        VALUES (10, 1, '/show/1', 'Episode 1', 1, 1), (11, 1, '/show/2', 'Episode 2', 2, 0)
                        """,
                        """
                        INSERT INTO entry_progress_state(entry_id, chapter_id, resource_key, locator_kind, position)
                        VALUES (1, 10, '/show/1', 'time', 1300), (1, 11, '/show/2', 'time', 600)
                        """,
                    )
                    exchange.settle(listOf(a, b))

                    a.sql("UPDATE entries SET profile_id = 2 WHERE _id = 1")
                    exchange.settle(listOf(a, b))

                    a.library() shouldBe b.library()
                    b.library() shouldContainAll listOf(
                        """["entry","work","aniyomi",777,"/show","anime",1,0,null,"",0,0,0]""",
                        """["progress","/show","","/show/1",1300,0]""",
                        """["progress","/show","","/show/2",600,0]""",
                    )
                    b.rows(
                        """
                        SELECT json_array(E.url, C.url)
                        FROM entry_progress_state R JOIN entries E ON E._id = R.entry_id
                        LEFT JOIN chapters C ON C._id = R.chapter_id AND C.entry_id = R.entry_id
                        ORDER BY R.resource_key
                        """,
                    ) shouldBe listOf("""["/show","/show/1"]""", """["/show","/show/2"]""")
                }
            }
        }

    @Test
    fun `a profile move keeps the chapters' groups on the other device, so a chapter marked there keeps its group`():
        Unit = runBlocking {
        val exchange = RecordExchange()
        SyncTestDevice(directory.resolve("a.db")).use { a ->
            SyncTestDevice(directory.resolve("b.db")).use { b ->
                a.startSync(deviceId = 1)
                b.startSync(deviceId = 2)
                a.sql(
                    "INSERT INTO profiles(_id, uuid, name, color_seed, position) VALUES (2, 'work', 'Work', 1, 1)",
                    "INSERT INTO source_identities(local_key, id_scheme, external_id) VALUES (-2, 'aniyomi', 777)",
                    "INSERT INTO entries(_id, source, url, title, favorite, type) VALUES (1, -2, '/show', 'Show', 1, 'anime')",
                    "INSERT INTO chapter_groups(entry_id, group_key, title, position) VALUES (1, 's2', 'Season 2', 1)",
                    """
                        INSERT INTO chapters(_id, entry_id, url, name, chapter_number, read, group_key, group_position)
                        VALUES (10, 1, '/show/s2e1', 'Episode 1', 1, 1, 's2', 1)
                        """,
                )
                exchange.settle(listOf(a, b))
                b.sql("INSERT INTO expanded_chapter_groups(entry_id, group_key) SELECT _id, 's2' FROM entries")

                a.sql("UPDATE entries SET profile_id = 2 WHERE _id = 1")
                exchange.settle(listOf(a, b))

                b.rows(
                    """
                        SELECT json_array(P.uuid, C.url, G.title, C.group_position)
                        FROM chapters C
                        JOIN entries E ON E._id = C.entry_id
                        JOIN profiles P ON P._id = E.profile_id
                        JOIN chapter_groups G ON G.entry_id = C.entry_id AND G.group_key = C.group_key
                        """,
                ) shouldBe listOf("""["work","/show/s2e1","Season 2",1]""")
                b.rows(
                    """
                        SELECT json_array(P.uuid, X.group_key)
                        FROM expanded_chapter_groups X
                        JOIN entries E ON E._id = X.entry_id
                        JOIN profiles P ON P._id = E.profile_id
                        """,
                ) shouldBe listOf("""["work","s2"]""")

                b.sql("UPDATE chapters SET bookmark = 1")
                exchange.pushFrom(b).map { it.record }.filter { it.kind == ChapterKind.name }
                    .map { ChapterKind.decode(it.payload).details?.group } shouldBe
                    listOf(ChapterGroup(key = "s2", title = "Season 2", position = 1))
            }
        }
    }
}
