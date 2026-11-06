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
                    a.questions.open().first() shouldBe listOf(Question.ProfileDeletion("work"))
                    b.questions.open().first() shouldBe listOf(Question.ProfileDeletion("work"))

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
}
