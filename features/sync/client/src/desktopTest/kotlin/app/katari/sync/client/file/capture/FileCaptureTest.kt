package app.katari.sync.client.file.capture

import app.katari.sync.client.SyncTestDevice
import app.katari.sync.client.showRows
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class FileCaptureTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `a cover change the database failed to take is recorded at the next flush`(): Unit = runBlocking {
        SyncTestDevice(directory.resolve("device.db")).use { device ->
            device.sql(*showRows("/show"))
            device.startSync(deviceId = 1)
            device.clearOutbox()
            device.sql(
                "CREATE TRIGGER failing_outbox BEFORE INSERT ON sync_outbox BEGIN SELECT RAISE(ABORT, 'full'); END",
            )
            device.setCustomCover("/show", "cover".toByteArray())
            withTimeout(5_000) { device.fileCapture.flush() }
            device.outboxKinds().shouldBeEmpty()

            device.sql("DROP TRIGGER failing_outbox")
            withTimeout(5_000) { device.fileCapture.flush() }
            device.outboxKinds() shouldBe listOf("entry")
        }
    }
}
