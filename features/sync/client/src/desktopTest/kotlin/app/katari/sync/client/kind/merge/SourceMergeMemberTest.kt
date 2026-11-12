package app.katari.sync.client.kind.merge

import app.katari.sync.client.RecordExchange
import app.katari.sync.client.SyncTestDevice
import app.katari.sync.client.rows
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/** Titles the person adds to a merge a source made, beside the places the source gives on each device. */
class SourceMergeMemberTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `a title the person added to a source's merge joins it on the other device`(): Unit = runBlocking {
        withDevices { a, b, exchange ->
            b.refreshSource(listed = listOf("/season-1"))

            a.sql("INSERT INTO merged_entries(target_entry_id, entry_id, position) $X_AT_2")
            exchange.settle(listOf(a, b))

            b.merge() shouldBe listOf("/show", "/season-1", "/extra")
        }
    }

    @Test
    fun `the person's title moves a title the other device's source placed along, instead of replacing it`(): Unit =
        runBlocking {
            withDevices { a, b, exchange ->
                // B's source lists a season A's hasn't listed yet, at the place A's person put their title
                b.refreshSource(listed = listOf("/season-1", "/season-2"))

                a.sql("INSERT INTO merged_entries(target_entry_id, entry_id, position) $X_AT_2")
                exchange.settle(listOf(a, b))

                b.merge() shouldBe listOf("/show", "/season-1", "/extra", "/season-2")
            }
        }

    @Test
    fun `a device whose refresh hasn't made the source's merge yet gets one with the person's title`(): Unit =
        runBlocking {
            withDevices { a, b, exchange ->
                a.sql("INSERT INTO merged_entries(target_entry_id, entry_id, position) $X_AT_2")
                exchange.settle(listOf(a, b))

                b.merge() shouldBe listOf("/show", "/extra")
            }
        }

    @Test
    fun `a title the person merged and unmerged elsewhere keeps the place a source gave it here`(): Unit =
        runBlocking {
            withDevices { a, b, exchange ->
                b.refreshSource(listed = listOf("/season-1", "/extra"))

                a.sql(
                    """
                    INSERT INTO merged_entries(target_entry_id, entry_id, position)
                    SELECT _id, _id, 0 FROM entries WHERE url = '/season-2'
                    UNION ALL SELECT (SELECT _id FROM entries WHERE url = '/season-2'), _id, 1
                    FROM entries WHERE url = '/extra'
                    """,
                    "DELETE FROM merged_entries WHERE entry_id = (SELECT _id FROM entries WHERE url = '/extra')",
                )
                exchange.settle(listOf(a, b))

                b.merge() shouldBe listOf("/show", "/season-1", "/extra")
            }
        }

    /**
     * Devices A and B with a show its source merges from seasons, and a title of the same source beside it, all in the
     * Library on both. A's refresh made the show's merge of its first season.
     */
    private suspend fun withDevices(test: suspend (SyncTestDevice, SyncTestDevice, RecordExchange) -> Unit) {
        val exchange = RecordExchange()
        SyncTestDevice(directory.resolve("a.db")).use { a ->
            SyncTestDevice(directory.resolve("b.db")).use { b ->
                a.startSync(deviceId = 1)
                b.startSync(deviceId = 2)
                a.sql(
                    "INSERT INTO source_identities(local_key, id_scheme, external_id) VALUES (-2, 'aniyomi', 777)",
                    """
                    INSERT INTO entries(source, url, title, favorite, type)
                    VALUES (-2, '/show', 'Show', 1, 'anime'), (-2, '/season-1', 'Season 1', 1, 'anime'),
                        (-2, '/season-2', 'Season 2', 1, 'anime'), (-2, '/extra', 'Extra', 1, 'anime')
                    """,
                )
                exchange.settle(listOf(a, b))
                a.refreshSource(listed = listOf("/season-1"))
                exchange.settle(listOf(a, b))
                test(a, b, exchange)
            }
        }
    }

    /** What a refresh of the show does: its source merges the [listed] titles, in that order, after the show. */
    private suspend fun SyncTestDevice.refreshSource(listed: List<String>) {
        sql("UPDATE entries SET merged_by_source = 1 WHERE url = '/show'")
        listOf("/show").plus(listed).forEachIndexed { position, url ->
            if (position > 0) {
                sql(
                    """
                    INSERT INTO source_merge_listings(profile_id, root_entry_id, entry_id, position)
                    SELECT 1, (SELECT _id FROM entries WHERE url = '/show'), _id, $position
                    FROM entries WHERE url = '$url'
                    """,
                )
            }
            sql(
                """
                INSERT INTO merged_entries(target_entry_id, entry_id, position)
                SELECT (SELECT _id FROM entries WHERE url = '/show'), _id, $position FROM entries WHERE url = '$url'
                """,
            )
        }
    }

    private suspend fun SyncTestDevice.merge(): List<String> = rows(
        """
        SELECT E.url
        FROM merged_entries M JOIN entries E ON E._id = M.entry_id
        WHERE M.target_entry_id = (SELECT _id FROM entries WHERE url = '/show')
        ORDER BY M.position
        """,
    )

    private companion object {
        const val X_AT_2 = """
            SELECT (SELECT _id FROM entries WHERE url = '/show'), _id, 2 FROM entries WHERE url = '/extra'
        """
    }
}
