package app.katari.sync.client.status

import app.katari.sync.client.CallCutter
import app.katari.sync.client.SyncingTestDevice
import app.katari.sync.client.TestSyncServer
import app.katari.sync.client.run.SyncOutcome
import app.katari.sync.protocol.http.SyncHttp
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.time.Duration.Companion.seconds

class UnreachableStatusTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `retried runs don't hide a server found unreachable, and one that answers shows as reached at once`(): Unit =
        runBlocking {
            TestSyncServer(directory).use { server ->
                val cutter = CallCutter()
                SyncingTestDevice(directory.resolve("a.db"), cutter.client).use { a ->
                    a.join(server, server.account("reader"))
                    a.sync() shouldBe SyncOutcome.SYNCED
                    cutter.pullsBeforeCut = 0
                    a.sync() shouldBe SyncOutcome.UNREACHABLE
                    val since = a.connection.state().unreachableSince.shouldNotBeNull()

                    // The next run waits on a server that took the connection and hasn't answered, as a silent one does
                    val info = cutter.holdNext(SyncHttp.INFO)
                    a.runs.request()
                    a.runs.running.first { it }
                    a.status.status().first().activity shouldBe SyncStatus.Activity.Unreachable(since)

                    // It answers, and the run goes on to a pull that takes a while
                    val pull = cutter.holdNext(SyncHttp.PULL)
                    info.countDown()
                    withTimeout(10.seconds) {
                        a.status.status().first { it.activity is SyncStatus.Activity.Syncing }
                    }
                    pull.countDown()
                    withTimeout(10.seconds) {
                        a.status.status().first { it.activity !is SyncStatus.Activity.Syncing }
                    }.activity.shouldBeInstanceOf<SyncStatus.Activity.UpToDate>()
                }
            }
        }
}
