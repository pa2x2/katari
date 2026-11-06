package app.katari.sync.client.restore

import app.katari.sync.client.RecordExchange
import app.katari.sync.client.SyncTestDevice
import app.katari.sync.client.library
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class AccountRollbackTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `after a rollback every device has the backup's library, marks, history and categories`(): Unit =
        runBlocking {
            val exchange = RecordExchange()
            SyncTestDevice(directory.resolve("a.db")).use { a ->
                SyncTestDevice(directory.resolve("b.db")).use { b ->
                    a.startSync(deviceId = 1)
                    b.startSync(deviceId = 2)
                    // What the backup holds: the show in the library and in category Old, its episode bookmarked
                    a.sql(
                        "INSERT INTO source_identities(local_key, id_scheme, external_id) VALUES (-2, 'aniyomi', 777)",
                        """
                        INSERT INTO entries(_id, source, url, title, favorite, type)
                        VALUES (1, -2, '/show', 'Show', 1, 'anime')
                        """,
                        """
                        INSERT INTO chapters(_id, entry_id, url, name, chapter_number, bookmark)
                        VALUES (10, 1, '/show/1', 'Ep 1', 1, 1)
                        """,
                        "INSERT INTO categories(_id, name, sort, flags) VALUES (5, 'Old', 1, 0)",
                        "INSERT INTO entries_categories(profile_id, entry_id, category_id) VALUES (1, 1, 5)",
                    )
                    exchange.settle(listOf(a, b))

                    // Since the backup, on B: the episode watched, another show added, the show moved to New
                    b.sql(
                        "UPDATE chapters SET read = 1",
                        """
                        INSERT INTO history(entry_id, chapter_id, last_read, time_read)
                        SELECT entry_id, _id, 5000, 60 FROM chapters
                        """,
                        """
                        INSERT INTO entries(source, url, title, favorite, type)
                        SELECT local_key, '/other', 'Other', 1, 'anime' FROM source_identities WHERE external_id = 777
                        """,
                        "DELETE FROM entries_categories",
                        "DELETE FROM categories WHERE _id != 0",
                        "INSERT INTO categories(name, sort, flags) VALUES ('New', 1, 0)",
                        """
                        INSERT INTO entries_categories(profile_id, entry_id, category_id)
                        SELECT 1, E._id, C._id FROM entries E, categories C WHERE E.url = '/show' AND C.name = 'New'
                        """,
                    )
                    exchange.settle(listOf(a, b))
                    a.library() shouldContain """["history","/show/1",5000,60]"""

                    // A rolls back; the restore writes what the backup holds, as backup restore does
                    AccountRollback(a.handler).rollBack(setOf(1L)) {
                        a.sql(
                            "UPDATE entries SET favorite = 1 WHERE url = '/show'",
                            "UPDATE chapters SET bookmark = 1",
                            "INSERT INTO categories(name, sort, flags) VALUES ('Old', 1, 0)",
                            """
                            INSERT INTO entries_categories(profile_id, entry_id, category_id)
                            SELECT 1, E._id, C._id FROM entries E, categories C WHERE E.url = '/show' AND C.name = 'Old'
                            """,
                        )
                    }
                    exchange.settle(listOf(a, b))

                    a.library() shouldBe b.library()
                    b.library().filterNot { it.startsWith("[\"profile\"") } shouldContainExactlyInAnyOrder listOf(
                        """["entry","default","aniyomi",777,"/show","anime",1,0,null,"",0,0,0]""",
                        """["entry","default","aniyomi",777,"/other","anime",0,0,null,"",0,0,0]""",
                        """["chapter","/show","/show/1",0,1]""",
                        """["category","default","Old",1,0]""",
                        """["in category","/show","Old"]""",
                    )
                }
            }
        }
}
