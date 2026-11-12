package app.katari.sync.client.kind.question

import app.katari.sync.client.RecordExchange
import app.katari.sync.client.SyncTestDevice
import app.katari.sync.client.library
import app.katari.sync.client.question.Question
import app.katari.sync.client.rows
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ProfileDeletionQuestionTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `a profile deleted on one device while another changed it comes back until the person decides`(): Unit =
        runBlocking {
            val exchange = RecordExchange()
            SyncTestDevice(directory.resolve("a.db")).use { a ->
                SyncTestDevice(directory.resolve("b.db")).use { b ->
                    a.startSync(deviceId = 1)
                    b.startSync(deviceId = 2)
                    a.sql(
                        "INSERT INTO profiles(_id, uuid, name, color_seed, position) VALUES (2, 'work', 'Work', 1, 1)",
                        "INSERT INTO source_identities(local_key, id_scheme, external_id) VALUES (-2, 'aniyomi', 777)",
                        """
                        INSERT INTO entries(_id, profile_id, source, url, title, favorite, type)
                        VALUES (1, 2, -2, '/show', 'Show', 1, 'anime')
                        """,
                    )
                    exchange.settle(listOf(a, b))

                    a.sql("DELETE FROM profiles WHERE uuid = 'work'")
                    a.drainAll()
                    delay(5)
                    b.sql("UPDATE entries SET notes = 'Kept for later'")
                    exchange.settle(listOf(a, b))

                    val entry = """["entry","work","aniyomi",777,"/show","anime",1,0,null,"Kept for later",0,0,0]"""
                    a.library() shouldBe b.library()
                    a.library() shouldContain entry
                    val asked = a.questions.open().first().map { (it as Question.ProfileDeletion).profileUuid }
                    asked shouldBe listOf("work")
                    b.questions.open().first() shouldBe a.questions.open().first()

                    b.questions.answerProfileDeletion("work", ProfileDeletionAnswer.DELETE)
                    exchange.settle(listOf(a, b))

                    a.questions.open().first().shouldBeEmpty()
                    b.questions.open().first().shouldBeEmpty()
                    a.library() shouldBe b.library()
                    a.library() shouldNotContain entry
                    a.rows("SELECT uuid FROM profiles") shouldBe listOf("default")
                }
            }
        }

    @Test
    fun `a profile kept after its deletion elsewhere comes back with its categories on every device`(): Unit =
        runBlocking {
            val exchange = RecordExchange()
            SyncTestDevice(directory.resolve("a.db")).use { a ->
                SyncTestDevice(directory.resolve("b.db")).use { b ->
                    a.startSync(deviceId = 1)
                    b.startSync(deviceId = 2)
                    a.sql(
                        "INSERT INTO profiles(_id, uuid, name, color_seed, position) VALUES (2, 'work', 'Work', 1, 1)",
                        "INSERT INTO source_identities(local_key, id_scheme, external_id) VALUES (-2, 'aniyomi', 777)",
                        """
                        INSERT INTO entries(_id, profile_id, source, url, title, favorite, type)
                        VALUES (1, 2, -2, '/show', 'Show', 1, 'anime')
                        """,
                        "INSERT INTO categories(_id, profile_id, name, sort, flags) VALUES (5, 2, 'Later', 1, 0)",
                        "INSERT INTO entries_categories(profile_id, entry_id, category_id) VALUES (2, 1, 5)",
                        "INSERT INTO category_update_rules(profile_id, category_id, auto_update) VALUES (2, 5, 0)",
                    )
                    exchange.settle(listOf(a, b))

                    // As the app deletes a profile: its entries first, then the profile with the rest of its rows
                    a.sql("DELETE FROM entries WHERE profile_id = 2", "DELETE FROM profiles WHERE uuid = 'work'")
                    a.drainAll()
                    delay(5)
                    b.sql("UPDATE entries SET notes = 'Kept for later'")
                    exchange.settle(listOf(a, b))
                    // Answered on the device that deleted it
                    a.questions.answerProfileDeletion("work", ProfileDeletionAnswer.KEEP)
                    exchange.settle(listOf(a, b))

                    a.questions.open().first().shouldBeEmpty()
                    a.library() shouldBe b.library()
                    a.library() shouldContain """["category","work","Later",1,0]"""
                    a.library() shouldContain """["in category","/show","Later"]"""
                    val rules = """
                        SELECT json_array(P.uuid, C.name, R.auto_update)
                        FROM category_update_rules R
                        JOIN profiles P ON P._id = R.profile_id
                        JOIN categories C ON C._id = R.category_id
                    """
                    a.rows(rules) shouldBe listOf("""["work","Later",0]""")
                    b.rows(rules) shouldBe a.rows(rules)
                }
            }
        }
}
