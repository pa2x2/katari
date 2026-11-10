package app.katari.sync.client.readelsewhere

import app.katari.sync.client.RecordExchange
import app.katari.sync.client.SyncTestDevice
import app.katari.sync.client.kind.chapter.ChapterKind
import app.katari.sync.client.payload.Stamped
import app.katari.sync.client.rows
import app.katari.sync.client.showRows
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ChaptersReadElsewhereTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `chapters another device read or marked read are told apart, only when unread here, and once`(): Unit =
        runBlocking {
            val exchange = RecordExchange()
            SyncTestDevice(directory.resolve("a.db")).use { a ->
                SyncTestDevice(directory.resolve("b.db")).use { b ->
                    a.startSync(deviceId = 1)
                    b.startSync(deviceId = 2)
                    a.sql(*showRows("/show", episodes = 3))
                    b.sql(*showRows("/show", episodes = 3, read = 1))
                    a.sql("UPDATE chapters SET read = 1 WHERE url IN ('/show/1', '/show/2')")
                    a.markRead("/show/3")
                    val readByA = exchange.pushFrom(a)
                    exchange.deliver(b, readByA)
                    exchange.deliver(b, readByA)

                    b.readElsewhere shouldBe listOf(
                        ChaptersReadElsewhere(
                            entryId = b.entryId("/show"),
                            readIds = setOf(b.chapterId("/show/2")),
                            markedIds = setOf(b.chapterId("/show/3")),
                        ),
                    )
                    b.markedRead() shouldBe listOf("/show/3")
                }
            }
        }

    @Test
    fun `a mark an older device kept next to its newer read counts as reading`(): Unit = runBlocking {
        val exchange = RecordExchange()
        SyncTestDevice(directory.resolve("a.db")).use { a ->
            SyncTestDevice(directory.resolve("b.db")).use { b ->
                a.startSync(deviceId = 1)
                b.startSync(deviceId = 2)
                a.sql(*showRows("/show"))
                b.sql(*showRows("/show"))
                a.markRead("/show/1")

                // As a version without the mark rewrites the record when the person reads the chapter again there
                val records = exchange.pushFrom(a).map { record ->
                    if (record.record.kind != ChapterKind.name) return@map record
                    val payload = ChapterKind.decode(record.record.payload)
                    val later = payload.read.at.copy(counter = payload.read.at.counter + 1)
                    val readAgain = ChapterKind.encode(payload.copy(read = Stamped(true, later)), previous = null)
                    record.copy(record = record.record.copy(payload = readAgain))
                }
                exchange.deliver(b, records)

                b.readElsewhere.single().markedIds.shouldBeEmpty()
                b.readElsewhere.single().readIds shouldBe setOf(b.chapterId("/show/1"))
                b.markedRead().shouldBeEmpty()
            }
        }
    }

    /** Marks the chapter read as the entry screen's "Mark as read" does. */
    private suspend fun SyncTestDevice.markRead(url: String) = sql(
        "UPDATE chapters SET read = 1 WHERE url = '$url'",
        "INSERT INTO chapters_marked_read(chapter_id) SELECT _id FROM chapters WHERE url = '$url'",
    )

    private suspend fun SyncTestDevice.chapterId(url: String) =
        rows("SELECT _id FROM chapters WHERE url = '$url'").single().toLong()

    private suspend fun SyncTestDevice.markedRead() =
        rows("SELECT url FROM chapters JOIN chapters_marked_read ON chapter_id = _id ORDER BY url")
}
