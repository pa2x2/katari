package app.katari.sync.client.kind.question

import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.katari.sync.client.RecordExchange
import app.katari.sync.client.SyncTestDevice
import app.katari.sync.client.kind.replacement.EntryReplacementKind
import app.katari.sync.client.library
import app.katari.sync.client.question.Question
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
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
    fun `migrations to two entries ask once, and the answer settles both devices and undoes the other`(): Unit =
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
                        """
                        INSERT INTO entries(source, url, title, favorite, type, date_added, notes)
                        VALUES (-2, '/old', 'Show', 1, 'anime', 100, 'Old notes'),
                            (-2, '/old2', 'Other show', 1, 'anime', 100, 'Old notes')
                        """,
                    )
                    b.sql(
                        "INSERT INTO source_identities(local_key, id_scheme, external_id) VALUES (-4, 'aniyomi', 999)",
                        // Already in the library before B migrates to it
                        """
                        INSERT INTO entries(source, url, title, favorite, type, date_added, notes)
                        VALUES (-4, '/there', 'Show', 1, 'anime', 500, 'Mine')
                        """,
                    )
                    exchange.settle(listOf(a, b))

                    a.migrate(from = "/old", source = -3, url = "/here")
                    delay(5)
                    b.migrate(from = "/old", source = -4, url = "/there")
                    exchange.settle(listOf(a, b))

                    val asked = a.questions.open().first()
                    asked shouldBe b.questions.open().first()
                    asked shouldHaveSize 1
                    val question = asked.single().shouldBeInstanceOf<Question.Migration>()
                    question.targets shouldHaveSize 2
                    a.library() shouldBe b.library()

                    // The earlier migration, which neither device follows yet
                    val kept = question.targets.map { it.value }.first { it != question.provisional }
                    b.questions.keepMigration(question.from, kept)
                    exchange.settle(listOf(a, b))

                    a.questions.open().first().shouldBeEmpty()
                    b.questions.open().first().shouldBeEmpty()
                    a.library() shouldBe b.library()
                    a.migratedTo(question.from) shouldBe kept
                    b.migratedTo(question.from) shouldBe kept
                    // The entry B migrated to is back as it was in the library before
                    a.library() shouldContain
                        """["entry","default","aniyomi",999,"/there","anime",1,500,null,"Mine",0,0,0]"""

                    a.migrate(from = "/old2", source = -3, url = "/here2")
                    delay(5)
                    b.migrate(from = "/old2", source = -4, url = "/there2")
                    exchange.settle(listOf(a, b))
                    val second = a.questions.open().first().single().shouldBeInstanceOf<Question.Migration>()
                    val turnedDown = second.targets.map { it.value }.first { it != second.provisional }
                    a.questions.keepMigration(second.from, turnedDown)
                    exchange.settle(listOf(a, b))

                    a.library() shouldBe b.library()
                    // B's migration put this entry in the library; turned down, it leaves it as migrating away would
                    a.library() shouldContain
                        """["entry","default","aniyomi",999,"/there2","anime",0,0,null,"Old notes",0,0,0]"""
                    a.library() shouldContain
                        """["entry","default","aniyomi",888,"/here2","anime",1,100,null,"Old notes",0,0,0]"""
                }
            }
        }

    private suspend fun SyncTestDevice.migratedTo(from: String): String = handler.await {
        val record = sync_recordsQueries.getByKey(EntryReplacementKind.key(from)).awaitAsOne()
        EntryReplacementKind.decode(record.payload).replacement.value.to
    }

    /**
     * Migrates [from] to the entry at [url] as the app does, keeping notes: the operation is recorded first, then the
     * target joins the library with the old entry's date added and notes, and the old entry leaves it. The target is
     * created first unless it is there already.
     */
    private suspend fun SyncTestDevice.migrate(from: String, source: Int, url: String) {
        sql(
            """
            INSERT INTO entries(source, url, title, favorite, type)
            SELECT $source, '$url', 'Show', 0, 'anime' WHERE NOT EXISTS (SELECT 1 FROM entries WHERE url = '$url')
            """,
            """
            INSERT INTO entry_migration_operations(
                operation_id, intent_fingerprint, profile_id, source_entry_id, target_entry_id, mode, created_at
            )
            SELECT '$url', 'REPLACE:NOTES', 1, O._id, N._id, 'REPLACE', 0
            FROM entries O, entries N WHERE O.url = '$from' AND N.url = '$url'
            """,
            """
            UPDATE entries SET favorite = 1,
                date_added = (SELECT date_added FROM entries WHERE url = '$from'),
                notes = (SELECT notes FROM entries WHERE url = '$from')
            WHERE url = '$url'
            """,
            "UPDATE entries SET favorite = 0, library_pinned = 0, date_added = 0 WHERE url = '$from'",
        )
        drainAll()
    }
}
