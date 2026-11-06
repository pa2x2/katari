package app.katari.sync.client.kind.category

import app.katari.sync.client.RecordExchange
import app.katari.sync.client.SyncTestDevice
import app.katari.sync.client.library
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class CategoryMembershipTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `categories from two devices keep both, merge by name, and a deleted one's entries go to Default`(): Unit =
        runBlocking {
            val exchange = RecordExchange()
            SyncTestDevice(directory.resolve("a.db")).use { a ->
                SyncTestDevice(directory.resolve("b.db")).use { b ->
                    a.startSync(deviceId = 1)
                    b.startSync(deviceId = 2)
                    a.sql(
                        "INSERT INTO source_identities(local_key, id_scheme, external_id) VALUES (-2, 'aniyomi', 777)",
                        """
                        INSERT INTO entries(_id, source, url, title, favorite, type)
                        VALUES (1, -2, '/one', 'One', 1, 'anime'), (2, -2, '/two', 'Two', 1, 'anime'),
                            (3, -2, '/three', 'Three', 1, 'anime')
                        """,
                        "INSERT INTO categories(_id, name, sort, flags) VALUES (1, 'Watching', 1, 0), (2, 'Later', 2, 0)",
                    )
                    exchange.settle(listOf(a, b))

                    a.sql(
                        "INSERT INTO entries_categories(profile_id, entry_id, category_id) VALUES (1, 1, 1)",
                        "INSERT INTO categories(name, sort, flags) VALUES ('Done', 3, 0)",
                        """
                        INSERT INTO entries_categories(profile_id, entry_id, category_id)
                        SELECT 1, 1, _id FROM categories WHERE name = 'Done'
                        """,
                        "DELETE FROM categories WHERE name = 'Later'",
                    )
                    a.drainAll()
                    delay(5)
                    b.sql(
                        """
                        INSERT INTO entries_categories(profile_id, entry_id, category_id)
                        SELECT 1, E._id, C._id FROM entries E, categories C
                        WHERE (E.url = '/two' AND C.name = 'Watching') OR (E.url = '/three' AND C.name = 'Later')
                        """,
                        "INSERT INTO categories(name, sort, flags) VALUES ('Done', 3, 0)",
                        """
                        INSERT INTO entries_categories(profile_id, entry_id, category_id)
                        SELECT 1, E._id, C._id FROM entries E, categories C WHERE E.url = '/two' AND C.name = 'Done'
                        """,
                    )

                    exchange.settle(listOf(a, b))

                    a.library() shouldBe b.library()
                    a.library().filter { it.startsWith("""["category"""") } shouldBe listOf(
                        """["category","default","Done",3,0]""",
                        """["category","default","Watching",1,0]""",
                    )
                    a.library() shouldContainAll listOf(
                        """["in category","/one","Watching"]""",
                        """["in category","/two","Watching"]""",
                        """["in category","/one","Done"]""",
                        """["in category","/two","Done"]""",
                    )
                    a.library().filter { it.startsWith("""["in category","/three"""") } shouldBe emptyList()
                    a.library() shouldNotContain """["category","default","Later",2,0]"""
                }
            }
        }
}
