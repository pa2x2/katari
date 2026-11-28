package app.katari.sync.client.outbox

import app.cash.sqldelight.Query
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.katari.sync.client.SyncTestDevice
import app.katari.sync.client.kind.chapter.ChapterKind
import app.katari.sync.client.kind.entry.EntryKind
import app.katari.sync.client.record.RecordGroups
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldEndWith
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.test.runTest
import okio.Buffer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class OutboxDrainTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `an edit keeps the fields a newer app version added, and an undone edit pushes nothing`() = runTest {
        SyncTestDevice(directory.resolve("device.db")).use { device ->
            device.startSync(deviceId = 1)
            device.sql(
                "INSERT INTO source_identities(local_key, id_scheme, external_id) VALUES (-2, 'aniyomi', 777)",
                """
                INSERT INTO entries(_id, source, url, title, favorite, type)
                VALUES (1, -2, '/show', 'Show', 1, 'anime')
                """,
            )
            device.drainAll()
            val key = device.entryRecord().record_key
            // Field 1000 as a newer version would write it: a length-delimited value this version has no property for,
            // after a tag (1000 shl 3 or 2) that takes two varint bytes.
            val newerField = Buffer().writeByte(0xC2).writeByte(0x3E).writeByte(5).writeUtf8("newer").readByteArray()
            device.handler.await {
                sync_recordsQueries.storeLocalChange(
                    recordKey = key,
                    kind = EntryKind.name,
                    localRef = "[1]",
                    kindVersion = 1,
                    payload = device.entryRecord().payload + newerField,
                    deleted = false,
                    changeId = null,
                    recordGroup = RecordGroups.entry(key),
                )
            }

            device.sql("UPDATE entries SET notes = 'Notes' WHERE _id = 1")
            device.drainAll()
            val edited = device.entryRecord()
            edited.change_id shouldNotBe null
            edited.payload.toList() shouldEndWith newerField.toList()
            EntryKind.decode(edited.payload).notes.value shouldBe "Notes"

            device.handler.await {
                sync_recordsQueries.markPushed(sequence = 4, changeId = edited.change_id!!, recordKey = key)
            }
            device.sql(
                "UPDATE entries SET notes = 'Draft' WHERE _id = 1",
                "UPDATE entries SET notes = 'Notes' WHERE _id = 1",
            )
            device.drainAll()
            device.entryRecord().change_id shouldBe null
        }
    }

    @Test
    fun `a drain with nothing to read again leaves the outbox's listeners alone`() = runTest {
        SyncTestDevice(directory.resolve("device.db")).use { device ->
            device.startSync(deviceId = 1)
            device.sql(
                "INSERT INTO source_identities(local_key, id_scheme, external_id) VALUES (-2, 'aniyomi', 777)",
                """
                INSERT INTO entries(_id, source, url, title, favorite, type)
                VALUES (1, -2, '/show', 'Show', 1, 'anime')
                """,
            )
            device.drainAll()
            var heard = 0
            val listener = Query.Listener { heard++ }
            device.driver.addListener("sync_outbox", listener = listener)

            device.drain.drain() shouldBe 0

            device.driver.removeListener("sync_outbox", listener = listener)
            heard shouldBe 0
        }
    }

    @Test
    fun `a chapter read in a profile this device doesn't sync is held back with the entry stored along with it`() =
        runTest {
            SyncTestDevice(directory.resolve("device.db")).use { device ->
                device.syncPreferences.profilesOff.set(setOf("work"))
                device.startSync(deviceId = 1)
                // An entry out of the library is recorded only along with its read chapter, in the same drain
                device.sql(
                    "INSERT INTO profiles(_id, uuid, name, color_seed, position) VALUES (2, 'work', 'Work', 1, 1)",
                    "INSERT INTO source_identities(local_key, id_scheme, external_id) VALUES (-2, 'aniyomi', 777)",
                    """
                    INSERT INTO entries(_id, profile_id, source, url, title, favorite, type)
                    VALUES (1, 2, -2, '/show', 'Show', 0, 'anime')
                    """,
                    """
                    INSERT INTO chapters(entry_id, url, name, chapter_number, read)
                    VALUES (1, '/show/1', 'Episode 1', 1, 1)
                    """,
                )
                device.drainAll()

                val heldBack = device.handler.await { sync_recordsQueries.heldBack().awaitAsList() }
                heldBack.map { it.kind } shouldContainAll listOf(EntryKind.name, ChapterKind.name)
                device.pending.next(Long.MAX_VALUE) shouldBe emptyList()
            }
        }

    private suspend fun SyncTestDevice.entryRecord() =
        handler.await { sync_recordsQueries.getByLocalRef(EntryKind.name, "[1]").awaitAsOne() }
}
