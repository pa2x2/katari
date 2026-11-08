package app.katari.sync.client.handoff

import app.katari.sync.client.SyncingTestDevice
import app.katari.sync.client.TestSyncServer
import app.katari.sync.client.rows
import app.katari.sync.client.run.SyncOutcome
import app.katari.sync.client.showRows
import app.katari.sync.protocol.http.SyncHttp
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.concurrent.CountDownLatch
import kotlin.time.Duration.Companion.seconds

class EntryHandOffCheckTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `an entry opened during a run is checking at once and the next run settles it`(): Unit = runBlocking {
        withProgressOnB { a, entryId, gate ->
            a.runs.request()
            gate.reached.await()
            a.triggers.entryOpened(entryId)
            withTimeout(2.seconds) { a.handOff.checks.first { it[entryId] == HandOffCheck.Checking } }

            gate.open()
            val check = withTimeout(20.seconds) { a.handOff.checks.first { it[entryId] is HandOffCheck.Checked } }
            check[entryId].shouldBeInstanceOf<HandOffCheck.Checked>()
            a.local.rows("SELECT position FROM entry_progress_state") shouldBe listOf("600")
        }
    }

    @Test
    fun `a check behind a server that never answers is told unreachable within its limit`(): Unit = runBlocking {
        withProgressOnB { a, entryId, gate ->
            a.runs.request()
            gate.reached.await()
            a.triggers.entryOpened(entryId)
            withTimeout(EntryHandOff.CHECK_LIMIT + 2.seconds) {
                a.handOff.checks.first { it[entryId] == HandOffCheck.Unreachable }
            }
            gate.open()
        }
    }

    /** A and B share a show; B has saved progress A hasn't pulled, and A's pulls wait at [PullGate] once it holds. */
    private suspend fun withProgressOnB(block: suspend (SyncingTestDevice, Long, PullGate) -> Unit) {
        TestSyncServer(directory).use { server ->
            val account = server.account("reader")
            val gate = PullGate()
            SyncingTestDevice(directory.resolve("a.db"), gate.client).use { a ->
                SyncingTestDevice(directory.resolve("b.db")).use { b ->
                    // Watched, so the episodes sync and B has them to save progress in
                    a.sql(*showRows("/show", episodes = 2, read = 2))
                    a.join(server, account)
                    a.sync() shouldBe SyncOutcome.SYNCED
                    b.join(server, account)
                    b.sync() shouldBe SyncOutcome.SYNCED
                    b.sql(
                        """
                        INSERT INTO entry_progress_state(entry_id, chapter_id, resource_key, locator_kind, position)
                        SELECT entry_id, _id, url, 'time', 600 FROM chapters WHERE url = '/show/2'
                        """,
                    )
                    b.sync() shouldBe SyncOutcome.SYNCED

                    gate.hold()
                    val entryId = a.local.entryId("/show")
                    try {
                        block(a, entryId, gate)
                    } finally {
                        gate.open()
                    }
                }
            }
        }
    }

    /** Holds every pull until [open], as a server that took the connection but never answers. */
    private class PullGate : Interceptor {
        @Volatile
        private var holding = false
        private val released = CountDownLatch(1)

        /** Completes when a held pull first waits. */
        val reached = CompletableDeferred<Unit>()

        val client: OkHttpClient = OkHttpClient.Builder().addInterceptor(this).build()

        fun hold() {
            holding = true
        }

        fun open() {
            holding = false
            released.countDown()
        }

        override fun intercept(chain: Interceptor.Chain): Response {
            if (holding && chain.request().url.encodedPath.endsWith(SyncHttp.PULL)) {
                reached.complete(Unit)
                released.await()
            }
            return chain.proceed(chain.request())
        }
    }
}
