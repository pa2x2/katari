package app.katari.sync.client.trigger

import app.katari.sync.client.CallCutter
import app.katari.sync.client.SyncingTestDevice
import app.katari.sync.client.TestSyncServer
import app.katari.sync.client.run.SyncOutcome
import app.katari.sync.client.showRows
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class LocalEditBurstTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `a burst of edits is sent in a push or two, not one per edit`(): Unit = runBlocking {
        TestSyncServer(directory).use { server ->
            val account = server.account("reader")
            val cutter = CallCutter()
            SyncingTestDevice(directory.resolve("a.db"), cutter.client).use { a ->
                a.sql(*showRows("/show", episodes = EPISODES))
                a.join(server, account)
                a.sync() shouldBe SyncOutcome.SYNCED
                a.triggers.start()
                cutter.pushes.set(0)

                // Marking episodes read one by one for 1.5 s, as the app's queries do: each write tells the listeners.
                (1..EPISODES).forEach { number ->
                    a.sql("UPDATE chapters SET read = 1 WHERE url = '/show/$number'")
                    a.local.driver.notifyListeners("chapters")
                    delay(50.milliseconds)
                }
                withTimeout(30.seconds) {
                    while (cutter.pushes.get() == 0 || a.local.pending.count() > 0) delay(100.milliseconds)
                }
                // The first edit goes at once, the rest together after the gap unless that run took them already
                cutter.pushes.get() shouldBeInRange 1..2
            }
        }
    }

    private companion object {
        const val EPISODES = 30
    }
}
