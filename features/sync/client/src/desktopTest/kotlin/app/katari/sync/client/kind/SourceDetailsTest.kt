package app.katari.sync.client.kind

import app.katari.sync.client.RecordExchange
import app.katari.sync.client.SyncTestDevice
import app.katari.sync.client.kind.chapter.ChapterKind
import app.katari.sync.client.kind.entry.EntryKind
import app.katari.sync.client.rows
import app.katari.sync.protocol.record.SequencedRecord
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tachiyomi.data.entry.EntryChapterRepositoryImpl
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
                    a.sql(*SHOW_FROM_SOURCE)

                    exchange.deliver(b, exchange.pushFrom(a))

                    b.rows("SELECT json_array(CAST(memo AS TEXT), update_strategy) FROM entries") shouldBe
                        listOf(ENTRY_DETAILS)
                    b.rows(CHAPTER_DETAILS_QUERY) shouldBe listOf(CHAPTER_DETAILS)
                    b.rows("SELECT json_array(group_key, title, position) FROM chapter_groups") shouldBe
                        listOf("""["s2","Season 2",1]""")
                }
            }
        }

    @Test
    fun `edits of rows created without the source's details leave the details another device sends`(): Unit =
        runBlocking {
            val exchange = RecordExchange()
            SyncTestDevice(directory.resolve("a.db")).use { a ->
                SyncTestDevice(directory.resolve("b.db")).use { b ->
                    SyncTestDevice(directory.resolve("c.db")).use { c ->
                        a.startSync(deviceId = 1)
                        b.startSync(deviceId = 2)
                        c.startSync(deviceId = 3)
                        a.sql(*SHOW_FROM_SOURCE)
                        val fromSource = exchange.pushFrom(a)
                        exchange.deliver(b, withoutDetails(fromSource))

                        b.sql("UPDATE entries SET notes = 'Rewatch'", "UPDATE chapters SET read = 1")
                        exchange.deliver(c, exchange.pushFrom(b))
                        exchange.deliver(c, fromSource)

                        c.rows("SELECT json_array(CAST(memo AS TEXT), update_strategy) FROM entries") shouldBe
                            listOf(ENTRY_DETAILS)
                        c.rows(CHAPTER_DETAILS_QUERY) shouldBe listOf(CHAPTER_DETAILS)
                    }
                }
            }
        }

    @Test
    fun `rows created without the source's details take them from the first record that carries them`(): Unit =
        runBlocking {
            val exchange = RecordExchange()
            SyncTestDevice(directory.resolve("a.db")).use { a ->
                SyncTestDevice(directory.resolve("b.db")).use { b ->
                    a.startSync(deviceId = 1)
                    b.startSync(deviceId = 2)
                    a.sql(*SHOW_FROM_SOURCE)
                    val fromSource = exchange.pushFrom(a)
                    exchange.deliver(b, withoutDetails(fromSource))

                    exchange.deliver(b, fromSource)

                    b.rows("SELECT json_array(CAST(memo AS TEXT), update_strategy) FROM entries") shouldBe
                        listOf(ENTRY_DETAILS)
                    b.rows(CHAPTER_DETAILS_QUERY) shouldBe listOf(CHAPTER_DETAILS)
                    b.rows("SELECT json_array(group_key, title, position) FROM chapter_groups") shouldBe
                        listOf("""["s2","Season 2",1]""")
                }
            }
        }

    @Test
    fun `a refresh of rows created without the source's details passes them on once`(): Unit = runBlocking {
        val exchange = RecordExchange()
        SyncTestDevice(directory.resolve("a.db")).use { a ->
            SyncTestDevice(directory.resolve("b.db")).use { b ->
                SyncTestDevice(directory.resolve("c.db")).use { c ->
                    a.startSync(deviceId = 1)
                    b.startSync(deviceId = 2)
                    c.startSync(deviceId = 3)
                    a.sql(*SHOW_FROM_SOURCE)
                    val withoutDetails = withoutDetails(exchange.pushFrom(a))
                    exchange.deliver(b, withoutDetails)
                    val chapters = EntryChapterRepositoryImpl(b.handler, b.defaultProfile)
                    val chapterId = b.rows("SELECT _id FROM chapters").single().toLong()
                    suspend fun refresh() {
                        b.sql(
                            """
                            UPDATE entries
                            SET memo = CAST('{"slug":"show-x1"}' AS BLOB), update_strategy = 1, initialized = 1
                            """,
                            """UPDATE chapters SET memo = CAST('{"hash":"9f3c"}' AS BLOB)""",
                        )
                        chapters.applySourceSync(emptyList(), emptyList(), emptyList(), listedIds = listOf(chapterId))
                    }

                    refresh()
                    exchange.deliver(c, withoutDetails)
                    exchange.deliver(c, exchange.pushFrom(b))
                    refresh()

                    c.rows("SELECT json_array(CAST(memo AS TEXT), update_strategy) FROM entries") shouldBe
                        listOf(ENTRY_DETAILS)
                    c.rows("SELECT CAST(memo AS TEXT) FROM chapters") shouldBe listOf("""{"hash":"9f3c"}""")
                    exchange.pushFrom(b) shouldBe emptyList()
                }
            }
        }
    }

    /** [records] as a device on a release before records carried the source's details pushed them. */
    private fun withoutDetails(records: List<SequencedRecord>): List<SequencedRecord> = records.map { sequenced ->
        val record = sequenced.record
        val payload = when (record.kind) {
            EntryKind.name -> EntryKind.encode(EntryKind.decode(record.payload).copy(details = null), previous = null)
            ChapterKind.name ->
                ChapterKind.encode(ChapterKind.decode(record.payload).copy(details = null), previous = null)
            else -> record.payload
        }
        sequenced.copy(record = record.copy(payload = payload))
    }

    private companion object {
        /** A show in the Library and a release its source's refresh found, with all the source told of both. */
        val SHOW_FROM_SOURCE = arrayOf(
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

        const val ENTRY_DETAILS = """["{\"slug\":\"show-x1\"}",1]"""

        const val CHAPTER_DETAILS_QUERY = """
            SELECT json_array(CAST(memo AS TEXT), summary, preview_url, kind, group_key, group_position, date_text)
            FROM chapters
        """

        const val CHAPTER_DETAILS =
            """["{\"hash\":\"9f3c\"}","Recap","https://example.org/s2e4.jpg",1,"s2",1,"Yesterday"]"""
    }
}
