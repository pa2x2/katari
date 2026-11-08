package app.katari.sync.client.firstsync

import app.katari.sync.client.SyncingTestDevice
import app.katari.sync.client.TestSyncServer
import app.katari.sync.client.account.AccountCall
import app.katari.sync.client.connection.SyncStage
import app.katari.sync.client.run.SyncOutcome
import app.katari.sync.client.showRows
import app.katari.sync.client.status.SyncStatus
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import mihon.domain.profile.ProfileSeedPreferences
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class JoiningAccountTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `the first device of an empty account uploads everything it holds, and a new device takes it unasked`(): Unit =
        runBlocking {
            TestSyncServer(directory).use { server ->
                val account = server.account("reader")
                SyncingTestDevice(directory.resolve("a.db")).use { a ->
                    SyncingTestDevice(directory.resolve("b.db")).use { b ->
                        a.sql(*showRows("/show", episodes = 7, read = 4))
                        a.local.preferences.profiles.profileStore(1).getString(DISPLAY_MODE).set("list")
                        a.join(server, account)
                        a.sync() shouldBe SyncOutcome.SYNCED
                        a.connection.state().stage shouldBe SyncStage.READY

                        ProfileSeedPreferences.seed(b.local.preferences.profiles.profileStore(1))
                        b.join(server, account)
                        b.sync() shouldBe SyncOutcome.SYNCED
                        b.connection.state().stage shouldBe SyncStage.READY

                        b.library() shouldBe a.library()
                        b.library() shouldContain """["chapter","/show","/show/4",1,0]"""
                        b.local.preferences.profiles.profileStore(1).getString(DISPLAY_MODE).get() shouldBe "list"
                    }
                }
            }
        }

    @Test
    fun `a device with data of its own joining an account in use waits for the person's choices`(): Unit =
        runBlocking {
            TestSyncServer(directory).use { server ->
                val account = server.account("reader")
                SyncingTestDevice(directory.resolve("a.db")).use { a ->
                    SyncingTestDevice(directory.resolve("b.db")).use { b ->
                        a.sql(*showRows("/a-show", episodes = 2))
                        a.join(server, account)
                        a.sync() shouldBe SyncOutcome.SYNCED
                        val accountRecords = a.client().snapshot().recordCount

                        b.sql(*showRows("/b-show", episodes = 2, read = 1))
                        val before = b.library()
                        b.join(server, account)
                        b.sync() shouldBe SyncOutcome.IDLE
                        b.connection.state().stage shouldBe SyncStage.CHOOSING
                        b.status.status().first().activity shouldBe SyncStatus.Activity.WaitingForChoices
                        // Nothing merged either way while the person hasn't chosen
                        b.library() shouldBe before
                        b.client().snapshot().recordCount shouldBe accountRecords

                        val proposal = b.account.firstSyncProposal()
                            .shouldBeInstanceOf<AccountCall.Done<List<ProfilePairing>>>().value
                        b.account.answerFirstSync(
                            proposal.map { ProfileChoice(it.profileId, it.pairedWith?.uuid, SettingsWinner.DEVICE) },
                        )
                        b.sync() shouldBe SyncOutcome.SYNCED
                        b.connection.state().stage shouldBe SyncStage.READY
                        a.sync() shouldBe SyncOutcome.SYNCED

                        a.library() shouldBe b.library()
                        a.library() shouldContain """["chapter","/b-show","/b-show/1",1,0]"""
                        a.library() shouldContain
                            """["entry","default","aniyomi",777,"/a-show","anime",1,0,null,"",0,0,0]"""
                    }
                }
            }
        }

    private companion object {
        const val DISPLAY_MODE = "pref_display_mode_library"
    }
}
