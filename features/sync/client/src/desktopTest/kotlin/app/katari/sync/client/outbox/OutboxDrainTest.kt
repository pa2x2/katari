package app.katari.sync.client.outbox

import app.cash.sqldelight.Query
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.katari.sync.client.SyncTestDevice
import app.katari.sync.client.kind.entry.EntryKind
import app.katari.sync.client.record.RecordGroups
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
            // Field 16 as a newer version would write it: a length-delimited value this version has no property for,
            // after a tag (16 shl 3 or 2) that takes two varint bytes.
            val newerField = Buffer().writeByte(0x82).writeByte(0x01).writeByte(5).writeUtf8("newer").readByteArray()
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

    private suspend fun SyncTestDevice.entryRecord() =
        handler.await { sync_recordsQueries.getByLocalRef(EntryKind.name, "[1]").awaitAsOne() }
}
