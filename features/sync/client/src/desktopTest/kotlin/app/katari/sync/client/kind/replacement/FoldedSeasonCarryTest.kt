package app.katari.sync.client.kind.replacement

import app.katari.sync.client.RecordExchange
import app.katari.sync.client.SyncTestDevice
import app.katari.sync.client.library
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContainAnyOf
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class FoldedSeasonCarryTest {

    @TempDir
    lateinit var directory: File

    /**
     * Paired by number, an episode of a later season watched elsewhere after the migration marked the episode of that
     * number in the first season of the entry listing them all, and the season's viewer setting stayed its own.
     */
    @Test
    fun `a season folded in by position carries only its episodes' state, to the episodes paired`(): Unit =
        runBlocking {
            val exchange = RecordExchange()
            SyncTestDevice(directory.resolve("a.db")).use { a ->
                SyncTestDevice(directory.resolve("b.db")).use { b ->
                    a.startSync(deviceId = 1)
                    b.startSync(deviceId = 2)
                    a.sql(
                        """
                        INSERT INTO source_identities(local_key, id_scheme, external_id)
                        VALUES (-2, 'aniyomi', 777), (-3, 'aniyomi', 888)
                        """,
                        "INSERT INTO entries(_id, source, url, title, favorite, type) VALUES (1, -2, '/s2', 'Show S2', 1, 'anime')",
                        """
                        INSERT INTO chapters(_id, entry_id, url, name, chapter_number)
                        VALUES (10, 1, '/s2/1', 'Episode 1', 1), (11, 1, '/s2/2', 'Episode 2', 2),
                            (12, 1, '/s2/3', 'Episode 3', 3)
                        """,
                        """
                        INSERT INTO viewer_setting_overrides(entry_id, provider_id, setting_key, encoded_value, updated_at)
                        VALUES (1, 'player', 'speed', '1.0', 0)
                        """,
                        "INSERT INTO entries(_id, source, url, title, favorite, type) VALUES (2, -3, '/all', 'Show', 1, 'anime')",
                        """
                        INSERT INTO chapters(_id, entry_id, url, name, chapter_number)
                        VALUES (20, 2, '/all/1', 'Episode 1', 1), (21, 2, '/all/2', 'Episode 2', 2),
                            (22, 2, '/all/3', 'Episode 3', 3), (23, 2, '/all/4', 'Episode 4', 4),
                            (24, 2, '/all/5', 'Episode 5', 5), (25, 2, '/all/6', 'Episode 6', 6)
                        """,
                        """
                        INSERT INTO viewer_setting_overrides(entry_id, provider_id, setting_key, encoded_value, updated_at)
                        VALUES (2, 'player', 'speed', '1.25', 0)
                        """,
                    )
                    exchange.settle(listOf(a, b))
                    // B fetched both entries' episodes itself, as no one marked them yet
                    b.sql(
                        """
                        INSERT INTO chapters(entry_id, url, name, chapter_number)
                        SELECT E._id, E.url || '/' || N.number, 'Episode ' || N.number, N.number
                        FROM entries E
                        JOIN (SELECT 1 AS number UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) N
                        WHERE E.url = '/all' OR N.number <= 3
                        """,
                    )

                    // A folds the second season into the entry listing every season, after the first season's three
                    // episodes, as local migration does: it leaves the library, and the new entry keeps its viewer setting
                    a.sql(
                        "UPDATE entries SET favorite = 0 WHERE _id = 1",
                        """
                        INSERT INTO entry_migration_operations(
                            operation_id, intent_fingerprint, profile_id, source_entry_id, target_entry_id, mode,
                            chapter_state_only, created_at
                        )
                        VALUES ('session:1', 'REPLACE:CHILD_STATE', 1, 1, 2, 'REPLACE', 1, 0)
                        """,
                        """
                        INSERT INTO entry_migration_chapter_positions(
                            operation_id, position, source_chapter_url, target_chapter_url
                        )
                        VALUES ('session:1', 0, '/s2/1', '/all/4'), ('session:1', 1, '/s2/2', '/all/5'),
                            ('session:1', 2, '/s2/3', '/all/6')
                        """,
                    )
                    a.drainAll()
                    delay(5)
                    // B hasn't heard of it yet, watches the season's third episode and changes the season's speed
                    b.sql(
                        "UPDATE chapters SET read = 1 WHERE url = '/s2/3'",
                        """
                        INSERT INTO entry_progress_state(entry_id, chapter_id, resource_key, locator_kind, position)
                        SELECT entry_id, _id, '/s2/3', 'time', 600 FROM chapters WHERE url = '/s2/3'
                        """,
                        "UPDATE viewer_setting_overrides SET encoded_value = '1.5' WHERE encoded_value = '1.0'",
                    )

                    exchange.settle(listOf(a, b))

                    a.library() shouldBe b.library()
                    a.library() shouldContainAll listOf(
                        """["chapter","/all","/all/4",1,0]""",
                        """["chapter","/all","/all/5",1,0]""",
                        """["chapter","/all","/all/6",1,0]""",
                        """["progress","/all","","/all/6",600,0]""",
                        """["viewer setting","/all","player","speed","1.25"]""",
                        """["viewer setting","/s2","player","speed","1.5"]""",
                    )
                    a.library() shouldNotContainAnyOf listOf(
                        """["chapter","/all","/all/3",1,0]""",
                        """["progress","/all","","/all/3",600,0]""",
                    )
                }
            }
        }
}
