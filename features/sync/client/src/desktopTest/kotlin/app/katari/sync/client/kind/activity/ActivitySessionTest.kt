package app.katari.sync.client.kind.activity

import app.katari.sync.client.RecordExchange
import app.katari.sync.client.SyncTestDevice
import app.katari.sync.client.library
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ActivitySessionTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `sessions from two devices are all kept once, also when a page is delivered again`(): Unit = runBlocking {
        val exchange = RecordExchange()
        SyncTestDevice(directory.resolve("a.db")).use { a ->
            SyncTestDevice(directory.resolve("b.db")).use { b ->
                a.startSync(deviceId = 1)
                b.startSync(deviceId = 2)
                a.sql(
                    "INSERT INTO source_identities(local_key, id_scheme, external_id) VALUES (-2, 'aniyomi', 777)",
                    "INSERT INTO entries(_id, source, url, title, favorite, type) VALUES (1, -2, '/show', 'Show', 1, 'anime')",
                )
                exchange.settle(listOf(a, b))

                a.sql(
                    """
                    INSERT INTO activity_sessions(session_id, entry_id, started_at, ended_at, duration)
                    VALUES ('session-a', 1, 1000, 2000, 900)
                    """,
                )
                b.sql(
                    """
                    INSERT INTO activity_sessions(session_id, entry_id, started_at, ended_at, duration)
                    SELECT 'session-b', _id, 5000, 7000, 1800 FROM entries
                    """,
                )
                val fromA = exchange.pushFrom(a)
                // The first delivery's answer was lost, so the same page comes again
                exchange.deliver(b, fromA)
                exchange.deliver(b, fromA)
                exchange.settle(listOf(a, b))

                a.library() shouldBe b.library()
                a.library().filter { it.startsWith("""["session"""") } shouldBe listOf(
                    """["session","session-a","/show",1000,2000,900]""",
                    """["session","session-b","/show",5000,7000,1800]""",
                )
            }
        }
    }
}
