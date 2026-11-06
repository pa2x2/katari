package app.katari.sync.client.resync

import app.katari.sync.client.SyncingTestDevice
import app.katari.sync.client.TestSyncServer
import app.katari.sync.client.connection.SyncStage
import app.katari.sync.client.connection.SyncStop
import app.katari.sync.client.run.SyncOutcome
import app.katari.sync.client.showRows
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ReplacedServerTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `a server restored from an old copy gets back from the device what it lost`(): Unit = runBlocking {
        TestSyncServer(directory).use { server ->
            val account = server.account("reader")
            SyncingTestDevice(directory.resolve("a.db")).use { a ->
                SyncingTestDevice(directory.resolve("b.db")).use { b ->
                    a.sql(*showRows("/show", episodes = 3))
                    a.join(server, account)
                    a.sync() shouldBe SyncOutcome.SYNCED
                    val backup = server.backUp()

                    a.sql(*showRows("/later", episodes = 2, read = 2))
                    a.sql("UPDATE chapters SET read = 1 WHERE url = '/show/3'")
                    a.sync() shouldBe SyncOutcome.SYNCED
                    server.restoreFrom(backup)

                    a.sync() shouldBe SyncOutcome.SYNCED
                    a.connection.state().stage shouldBe SyncStage.READY

                    b.join(server, account)
                    b.sync() shouldBe SyncOutcome.SYNCED
                    b.library() shouldBe a.library()
                    b.library() shouldContain """["chapter","/later","/later/2",1,0]"""
                    b.library() shouldContain """["chapter","/show","/show/3",1,0]"""
                }
            }
        }
    }

    @Test
    fun `a new server under the same address asks to sign in again, then takes everything without questions`(): Unit =
        runBlocking {
            TestSyncServer(directory).use { server ->
                SyncingTestDevice(directory.resolve("a.db")).use { a ->
                    SyncingTestDevice(directory.resolve("b.db")).use { b ->
                        a.sql(*showRows("/show", episodes = 3, read = 3))
                        a.join(server, server.account("reader"))
                        a.sync() shouldBe SyncOutcome.SYNCED

                        server.replaceWithEmpty()
                        a.sync() shouldBe SyncOutcome.IDLE
                        a.connection.state().stopped shouldBe SyncStop.SIGN_IN_AGAIN
                        // Edits go on being captured while it can't sync.
                        a.sql(*showRows("/meanwhile", read = 1))

                        val account = server.account("reader")
                        a.join(server, account)
                        a.sync() shouldBe SyncOutcome.SYNCED
                        a.connection.state().let { state ->
                            state.stage shouldBe SyncStage.READY
                            state.stopped shouldBe null
                        }

                        b.join(server, account)
                        b.sync() shouldBe SyncOutcome.SYNCED
                        b.library() shouldBe a.library()
                        b.library() shouldContain """["chapter","/meanwhile","/meanwhile/1",1,0]"""
                        b.library() shouldContain """["chapter","/show","/show/3",1,0]"""
                    }
                }
            }
        }
}
