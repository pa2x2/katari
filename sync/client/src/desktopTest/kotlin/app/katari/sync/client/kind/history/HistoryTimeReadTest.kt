package app.katari.sync.client.kind.history

import app.katari.sync.client.RecordExchange
import app.katari.sync.client.SyncTestDevice
import app.katari.sync.client.library
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class HistoryTimeReadTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `time read on two devices at once adds up instead of the later one replacing the other`(): Unit = runBlocking {
        val exchange = RecordExchange()
        SyncTestDevice(directory.resolve("a.db")).use { a ->
            SyncTestDevice(directory.resolve("b.db")).use { b ->
                a.startSync(deviceId = 1)
                b.startSync(deviceId = 2)
                a.sql(
                    "INSERT INTO source_identities(local_key, id_scheme, external_id) VALUES (-2, 'aniyomi', 777)",
                    "INSERT INTO entries(_id, source, url, title, favorite, type) VALUES (1, -2, '/show', 'Show', 1, 'anime')",
                    "INSERT INTO chapters(_id, entry_id, url, name, chapter_number) VALUES (10, 1, '/show/1', 'Ep 1', 1)",
                    "INSERT INTO history(entry_id, chapter_id, last_read, time_read) VALUES (1, 10, 1000, 1000)",
                )
                exchange.settle(listOf(a, b))

                a.sql("UPDATE history SET time_read = time_read + 200, last_read = 2000")
                a.drainAll()
                b.sql("UPDATE history SET time_read = time_read + 500, last_read = 3000")
                exchange.settle(listOf(a, b))

                a.library() shouldBe b.library()
                a.library() shouldContain """["history","/show/1",3000,1700]"""
            }
        }
    }
}
