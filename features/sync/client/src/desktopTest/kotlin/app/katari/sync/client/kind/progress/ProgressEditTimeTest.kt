package app.katari.sync.client.kind.progress

import app.katari.sync.client.RecordExchange
import app.katari.sync.client.SyncTestDevice
import app.katari.sync.client.library
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ProgressEditTimeTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `a position written with an older reading time doesn't win over newer reading on another device`(): Unit =
        runBlocking {
            val exchange = RecordExchange()
            SyncTestDevice(directory.resolve("a.db")).use { a ->
                SyncTestDevice(directory.resolve("b.db")).use { b ->
                    a.startSync(deviceId = 1)
                    b.startSync(deviceId = 2)
                    a.sql(
                        "INSERT INTO source_identities(local_key, id_scheme, external_id) VALUES (-2, 'aniyomi', 777)",
                        "INSERT INTO entries(_id, source, url, title, favorite, type) VALUES (1, -2, '/book', 'Book', 1, 'book')",
                        "INSERT INTO chapters(_id, entry_id, url, name, chapter_number, read) VALUES (10, 1, '/book/1', 'Book', 1, 0)",
                        """
                        INSERT INTO entry_progress_state(entry_id, chapter_id, resource_key, locator_kind, position, locator_updated_at)
                        VALUES (1, 10, '/book/1', 'book', 10, 1000)
                        """,
                    )
                    exchange.settle(listOf(a, b))

                    // B reads on; later, A writes a position read before that, keeping when it was read, as moving a
                    // position to a renamed chapter or copying it in a migration does
                    b.sql("UPDATE entry_progress_state SET position = 900, locator_updated_at = 9000")
                    b.drainAll()
                    delay(5)
                    a.sql("UPDATE entry_progress_state SET position = 300, locator_updated_at = 3000")

                    exchange.settle(listOf(a, b))

                    b.library() shouldContain """["progress","/book","","/book/1",900,0]"""
                    a.library() shouldBe b.library()
                }
            }
        }
}
