package app.katari.sync.client.part

import app.katari.sync.client.SyncingTestDevice
import app.katari.sync.client.TestSyncServer
import app.katari.sync.client.rows
import app.katari.sync.client.run.SyncOutcome
import app.katari.sync.client.showRows
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ProfileChoiceTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `an account profile switched off leaves this device but not the account, and comes back whole`(): Unit =
        runBlocking {
            TestSyncServer(directory).use { server ->
                val account = server.account("reader")
                SyncingTestDevice(directory.resolve("a.db")).use { a ->
                    SyncingTestDevice(directory.resolve("b.db")).use { b ->
                        a.sql(
                            "INSERT INTO profiles(_id, uuid, name, color_seed, position) " +
                                "VALUES (2, 'work', 'Work', 1, 1)",
                            *showRows("/work", read = 1, profileId = 2),
                        )
                        a.join(server, account)
                        a.sync() shouldBe SyncOutcome.SYNCED
                        b.join(server, account)
                        b.sync() shouldBe SyncOutcome.SYNCED
                        val library = a.library()
                        b.library() shouldBe library

                        b.choiceChanges.leavesThisDevice("work") shouldBe true
                        b.choiceChanges.syncProfile("work", on = false)
                        b.local.rows("SELECT uuid FROM profiles") shouldBe listOf("default")
                        b.sync() shouldBe SyncOutcome.SYNCED
                        a.sync() shouldBe SyncOutcome.SYNCED
                        a.library() shouldBe library

                        b.choiceChanges.syncProfile("work", on = true)
                        b.library() shouldBe library
                    }
                }
            }
        }

    @Test
    fun `a profile made on another device waits for the person's answer, and comes here whole once added`(): Unit =
        runBlocking {
            TestSyncServer(directory).use { server ->
                val account = server.account("reader")
                SyncingTestDevice(directory.resolve("a.db")).use { a ->
                    SyncingTestDevice(directory.resolve("b.db")).use { b ->
                        a.join(server, account)
                        a.sync() shouldBe SyncOutcome.SYNCED
                        b.join(server, account)
                        b.sync() shouldBe SyncOutcome.SYNCED
                        b.preferences.askBeforeNewProfiles.set(true)

                        a.sql(
                            "INSERT INTO profiles(_id, uuid, name, color_seed, position) " +
                                "VALUES (2, 'work', 'Work', 1, 1)",
                            *showRows("/work", read = 1, profileId = 2),
                        )
                        a.sync() shouldBe SyncOutcome.SYNCED
                        b.sync() shouldBe SyncOutcome.SYNCED
                        b.preferences.profilesAsking.get() shouldBe setOf("work")
                        b.local.rows("SELECT uuid FROM profiles") shouldBe listOf("default")

                        b.choiceChanges.syncProfile("work", on = true)
                        b.preferences.profilesAsking.get() shouldBe emptySet()
                        b.library() shouldBe a.library()
                    }
                }
            }
        }
}
