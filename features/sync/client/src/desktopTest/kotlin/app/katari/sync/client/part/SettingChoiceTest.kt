package app.katari.sync.client.part

import app.katari.sync.client.SyncingTestDevice
import app.katari.sync.client.TestSyncServer
import app.katari.sync.client.firstsync.SettingsWinner
import app.katari.sync.client.run.SyncOutcome
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class SettingChoiceTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `a setting synced again keeping this device's value sends it to every device`(): Unit = runBlocking {
        TestSyncServer(directory).use { server ->
            val account = server.account("reader")
            SyncingTestDevice(directory.resolve("a.db")).use { a ->
                SyncingTestDevice(directory.resolve("b.db")).use { b ->
                    a.join(server, account)
                    a.sync() shouldBe SyncOutcome.SYNCED
                    b.join(server, account)
                    b.sync() shouldBe SyncOutcome.SYNCED
                    b.choiceChanges.syncSetting(ANIMATION_SPEED, on = false)

                    a.animationSpeed().set(250)
                    a.sync() shouldBe SyncOutcome.SYNCED
                    b.animationSpeed().set(1)
                    b.sync() shouldBe SyncOutcome.SYNCED
                    b.animationSpeed().get() shouldBe 1

                    b.choiceChanges.asksWhoseSetting(ANIMATION_SPEED) shouldBe true
                    b.choiceChanges.syncSetting(ANIMATION_SPEED, on = true, keep = SettingsWinner.DEVICE)
                    b.animationSpeed().get() shouldBe 1
                    b.sync() shouldBe SyncOutcome.SYNCED
                    a.sync() shouldBe SyncOutcome.SYNCED
                    a.animationSpeed().get() shouldBe 1
                }
            }
        }
    }

    @Test
    fun `a setting left alone while kept out takes the account's value back without asking`(): Unit = runBlocking {
        TestSyncServer(directory).use { server ->
            val account = server.account("reader")
            SyncingTestDevice(directory.resolve("a.db")).use { a ->
                SyncingTestDevice(directory.resolve("b.db")).use { b ->
                    a.join(server, account)
                    a.sync() shouldBe SyncOutcome.SYNCED
                    b.join(server, account)
                    b.sync() shouldBe SyncOutcome.SYNCED
                    b.choiceChanges.syncSetting(ANIMATION_SPEED, on = false)

                    a.animationSpeed().set(250)
                    a.sync() shouldBe SyncOutcome.SYNCED
                    b.sync() shouldBe SyncOutcome.SYNCED
                    b.animationSpeed().get() shouldBe 500

                    b.choiceChanges.asksWhoseSetting(ANIMATION_SPEED) shouldBe false
                    b.choiceChanges.syncSetting(ANIMATION_SPEED, on = true)
                    b.animationSpeed().get() shouldBe 250
                }
            }
        }
    }

    @Test
    fun `a setting the account never had keeps this device's value without asking`(): Unit = runBlocking {
        TestSyncServer(directory).use { server ->
            val account = server.account("reader")
            SyncingTestDevice(directory.resolve("a.db")).use { a ->
                SyncingTestDevice(directory.resolve("b.db")).use { b ->
                    a.join(server, account)
                    a.sync() shouldBe SyncOutcome.SYNCED
                    b.join(server, account)
                    b.sync() shouldBe SyncOutcome.SYNCED
                    b.choiceChanges.syncSetting(ANIMATION_SPEED, on = false)

                    b.animationSpeed().set(1)
                    b.sync() shouldBe SyncOutcome.SYNCED

                    b.choiceChanges.asksWhoseSetting(ANIMATION_SPEED) shouldBe false
                    b.choiceChanges.syncSetting(ANIMATION_SPEED, on = true)
                    b.animationSpeed().get() shouldBe 1
                    b.sync() shouldBe SyncOutcome.SYNCED
                    a.sync() shouldBe SyncOutcome.SYNCED
                    a.animationSpeed().get() shouldBe 1
                }
            }
        }
    }

    private fun SyncingTestDevice.animationSpeed() =
        local.preferences.profiles.profileStore(1).getInt(ANIMATION_SPEED, 500)

    private companion object {
        const val ANIMATION_SPEED = "pref_double_tap_anim_speed"
    }
}
