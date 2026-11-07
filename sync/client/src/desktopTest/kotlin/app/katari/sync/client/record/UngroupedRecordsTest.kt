package app.katari.sync.client.record

import app.katari.sync.client.SyncTestDevice
import app.katari.sync.client.rows
import app.katari.sync.client.showRows
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class UngroupedRecordsTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `records stored before groups were kept get the group each was stored with`(): Unit = runBlocking {
        SyncTestDevice(directory.resolve("a.db")).use { device ->
            device.startSync(deviceId = 1)
            device.sql(*showRows("/show", episodes = 2, read = 1))
            device.drainAll()
            val entryKey = device.rows("SELECT record_key FROM sync_records WHERE kind = 'entry'").single()
            val stored = device.groups()
            stored.values shouldContainAll listOf(entryKey)

            device.sql("UPDATE sync_records SET record_group = NULL")
            UngroupedRecords(device.handler).fill()

            device.groups() shouldBe stored
        }
    }

    private suspend fun SyncTestDevice.groups(): Map<String, String> =
        rows("SELECT record_key || ' ' || record_group FROM sync_records").associate {
            it.substringBefore(' ') to it.substringAfter(' ')
        }
}
