package app.katari.sync.client.apply

import app.katari.sync.client.SyncingTestDevice
import app.katari.sync.client.TestSyncServer
import app.katari.sync.client.run.SyncOutcome
import app.katari.sync.client.showRows
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class FailedApplyTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `a record that fails to apply is held aside while the rest sync, and applies after an update`(): Unit =
        runBlocking {
            TestSyncServer(directory).use { server ->
                val account = server.account("reader")
                SyncingTestDevice(directory.resolve("a.db")).use { a ->
                    SyncingTestDevice(directory.resolve("b.db")).use { b ->
                        a.sql(
                            *showRows("/good", episodes = 2, read = 2),
                            *showRows("/bad", episodes = 2, read = 2),
                            """
                            INSERT INTO history(entry_id, chapter_id, last_read, time_read)
                            SELECT entry_id, _id, 1000, 1000 FROM chapters WHERE url = '/bad/1'
                            """,
                        )
                        a.join(server, account)
                        a.sync() shouldBe SyncOutcome.SYNCED

                        // As a bug in this app version would, writing one chapter fails here
                        b.sql(
                            """
                            CREATE TRIGGER fail_bad BEFORE INSERT ON chapters WHEN NEW.url = '/bad/1'
                            BEGIN SELECT RAISE(ABORT, 'cannot take this chapter'); END
                            """,
                        )
                        b.join(server, account)
                        b.sync() shouldBe SyncOutcome.SYNCED
                        b.local.held.failedCount().first() shouldBe 1
                        b.library().let { library ->
                            library shouldContain """["chapter","/good","/good/2",1,0]"""
                            library shouldContain """["chapter","/bad","/bad/2",1,0]"""
                            library shouldNotContain """["chapter","/bad","/bad/1",1,0]"""
                            library.filter { it.startsWith("[\"history\"") } shouldBe emptyList()
                        }

                        // Later changes keep coming
                        a.sql(*showRows("/later", read = 1))
                        a.sync() shouldBe SyncOutcome.SYNCED
                        b.sync() shouldBe SyncOutcome.SYNCED
                        b.library() shouldContain """["chapter","/later","/later/1",1,0]"""

                        // The version it failed on doesn't try it again; an update does, and what waited for it follows
                        b.sql("DROP TRIGGER fail_bad")
                        b.sync() shouldBe SyncOutcome.SYNCED
                        b.local.held.failedCount().first() shouldBe 1
                        b.local.appVersion = "updated"
                        b.sync() shouldBe SyncOutcome.SYNCED
                        b.local.held.failedCount().first() shouldBe 0
                        b.library() shouldBe a.library()
                    }
                }
            }
        }
}
