package app.katari.sync.client.kind

import app.katari.sync.client.RecordExchange
import app.katari.sync.client.SyncTestDevice
import app.katari.sync.client.rows
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class SourceDetailsTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `an entry and a release another device's refresh found arrive as the source listed them`(): Unit =
        runBlocking {
            val exchange = RecordExchange()
            SyncTestDevice(directory.resolve("a.db")).use { a ->
                SyncTestDevice(directory.resolve("b.db")).use { b ->
                    a.startSync(deviceId = 1)
                    b.startSync(deviceId = 2)
                    a.sql(
                        "INSERT INTO source_identities(local_key, id_scheme, external_id) VALUES (-2, 'aniyomi', 777)",
                        """
                        INSERT INTO entries(_id, source, url, title, favorite, date_added, type, memo, update_strategy)
                        VALUES (1, -2, '/show', 'Show', 1, 1000, 'anime', CAST('{"slug":"show-x1"}' AS BLOB), 1)
                        """,
                        """
                        INSERT INTO chapter_groups(entry_id, group_key, title, position)
                        VALUES (1, 's1', 'Season 1', 0), (1, 's2', 'Season 2', 1)
                        """,
                        """
                        INSERT INTO chapters(
                            entry_id, url, name, source_order, date_fetch, memo, summary, preview_url, kind, group_key,
                            group_position, date_text
                        )
                        VALUES (
                            1, '/show/s2e4', 'Episode 4', 0, 2000, CAST('{"hash":"9f3c"}' AS BLOB), 'Recap',
                            'https://example.org/s2e4.jpg', 1, 's2', 1, 'Yesterday'
                        )
                        """,
                    )

                    exchange.deliver(b, exchange.pushFrom(a))

                    b.rows("SELECT json_array(CAST(memo AS TEXT), update_strategy) FROM entries") shouldBe
                        listOf("""["{\"slug\":\"show-x1\"}",1]""")
                    b.rows(
                        """
                        SELECT json_array(CAST(memo AS TEXT), summary, preview_url, kind, group_key, group_position,
                            date_text)
                        FROM chapters
                        """,
                    ) shouldBe listOf(
                        """["{\"hash\":\"9f3c\"}","Recap","https://example.org/s2e4.jpg",1,"s2",1,"Yesterday"]""",
                    )
                    b.rows("SELECT json_array(group_key, title, position) FROM chapter_groups") shouldBe
                        listOf("""["s2","Season 2",1]""")
                }
            }
        }
}
