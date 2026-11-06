package app.katari.sync.client.kind.question

import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.katari.sync.client.RecordExchange
import app.katari.sync.client.SyncTestDevice
import app.katari.sync.client.kind.replacement.EntryReplacementKind
import app.katari.sync.client.library
import app.katari.sync.client.question.Question
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class MigrationQuestionTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `an entry migrated to different entries on two devices asks once, and an answer on one settles both`(): Unit =
        runBlocking {
            val exchange = RecordExchange()
            SyncTestDevice(directory.resolve("a.db")).use { a ->
                SyncTestDevice(directory.resolve("b.db")).use { b ->
                    a.startSync(deviceId = 1)
                    b.startSync(deviceId = 2)
                    a.sql(
                        """
                        INSERT INTO source_identities(local_key, id_scheme, external_id)
                        VALUES (-2, 'aniyomi', 777), (-3, 'aniyomi', 888), (-4, 'aniyomi', 999)
                        """,
                        "INSERT INTO entries(_id, source, url, title, favorite, type) VALUES (1, -2, '/old', 'Show', 1, 'anime')",
                    )
                    exchange.settle(listOf(a, b))
                    b.sql(
                        "INSERT INTO source_identities(local_key, id_scheme, external_id) VALUES (-4, 'aniyomi', 999)",
                    )

                    a.migrate(source = -3, url = "/here")
                    delay(5)
                    b.migrate(source = -4, url = "/there")
                    exchange.settle(listOf(a, b))

                    val asked = a.questions.open().first()
                    asked shouldBe b.questions.open().first()
                    asked shouldHaveSize 1
                    val question = asked.single().shouldBeInstanceOf<Question.Migration>()
                    question.targets shouldHaveSize 2
                    a.library() shouldBe b.library()

                    // The earlier migration, which neither device follows yet
                    val kept = question.targets.first { it != question.provisional }
                    b.questions.keepMigration(question.from, kept)
                    exchange.settle(listOf(a, b))

                    a.questions.open().first().shouldBeEmpty()
                    b.questions.open().first().shouldBeEmpty()
                    a.library() shouldBe b.library()
                    a.migratedTo(question.from) shouldBe kept
                    b.migratedTo(question.from) shouldBe kept
                }
            }
        }

    private suspend fun SyncTestDevice.migratedTo(from: String): String = handler.await {
        val record = sync_recordsQueries.getByKey(EntryReplacementKind.key(from)).awaitAsOne()
        EntryReplacementKind.decode(record.payload).replacement.value.to
    }

    private suspend fun SyncTestDevice.migrate(source: Int, url: String) {
        sql(
            "INSERT INTO entries(source, url, title, favorite, type) VALUES ($source, '$url', 'Show', 1, 'anime')",
            "UPDATE entries SET favorite = 0 WHERE url = '/old'",
            """
            INSERT INTO entry_migration_operations(
                operation_id, intent_fingerprint, profile_id, source_entry_id, target_entry_id, mode, created_at
            )
            SELECT 'operation', 'fingerprint', 1, O._id, N._id, 'REPLACE', 0
            FROM entries O, entries N WHERE O.url = '/old' AND N.url = '$url'
            """,
        )
        drainAll()
    }
}
