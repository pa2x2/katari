package app.katari.sync.client.kind.subtitletiming

import app.katari.sync.client.RecordExchange
import app.katari.sync.client.SyncTestDevice
import app.katari.sync.client.library
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class SubtitleTimingTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `a timing reaches the other device, the later change wins, and resetting it removes it there`(): Unit =
        runBlocking {
            val exchange = RecordExchange()
            SyncTestDevice(directory.resolve("a.db")).use { a ->
                SyncTestDevice(directory.resolve("b.db")).use { b ->
                    a.startSync(deviceId = 1)
                    b.startSync(deviceId = 2)
                    a.sql(
                        "INSERT INTO source_identities(local_key, id_scheme, external_id) VALUES (-2, 'aniyomi', 777)",
                        "INSERT INTO entries(_id, source, url, title, favorite, type) VALUES (1, -2, '/show', 'Show', 1, 'anime')",
                        """
                        INSERT INTO chapters(_id, entry_id, url, name, chapter_number)
                        VALUES (10, 1, '/show/1', 'Episode 1', 1)
                        """,
                        """
                        INSERT INTO episode_subtitle_timings(chapter_id, subtitle_key, delay_ms)
                        VALUES (10, 'external:English', 400)
                        """,
                    )
                    exchange.settle(listOf(a, b))
                    b.library() shouldContain """["subtitle timing","/show/1","external:English",400]"""

                    a.sql("UPDATE episode_subtitle_timings SET delay_ms = 900")
                    a.drainAll()
                    delay(5)
                    b.sql("UPDATE episode_subtitle_timings SET delay_ms = -1500")
                    exchange.settle(listOf(a, b))
                    a.library() shouldBe b.library()
                    a.library() shouldContain """["subtitle timing","/show/1","external:English",-1500]"""

                    b.sql("DELETE FROM episode_subtitle_timings")
                    exchange.settle(listOf(a, b))
                    a.library() shouldBe b.library()
                    a.library().none { it.startsWith("""["subtitle timing"""") } shouldBe true
                }
            }
        }

    @Test
    fun `a timing changed on a moved entry by a device that hadn't heard of the move follows it to the new profile`():
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
                    "INSERT INTO chapters(_id, entry_id, url, name, chapter_number) VALUES (10, 1, '/show/1', 'Episode 1', 1)",
                    """
                    INSERT INTO episode_subtitle_timings(chapter_id, subtitle_key, delay_ms)
                    VALUES (10, 'external:English', 400)
                    """,
                )
                exchange.settle(listOf(a, b))

                a.sql("UPDATE entries SET profile_id = 2 WHERE _id = 1")
                a.drainAll()
                delay(5)
                b.sql("UPDATE episode_subtitle_timings SET delay_ms = 700")

                exchange.settle(listOf(a, b))

                a.library() shouldBe b.library()
                a.library() shouldContainAll listOf(
                    """["entry","work","aniyomi",777,"/show","anime",1,0,null,"",0,0,0]""",
                    """["subtitle timing","/show/1","external:English",700]""",
                )
                a.library() shouldNotContain """["subtitle timing","/show/1","external:English",400]"""
            }
        }
    }
}
