package app.katari.sync.client.part

import app.katari.sync.client.SyncingTestDevice
import app.katari.sync.client.TestSyncServer
import app.katari.sync.client.run.SyncOutcome
import app.katari.sync.client.setting.scope.SettingsArea
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class SettingsAreaChoiceTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `an area synced again takes the account's settings, and what the device changed meanwhile never leaves it`():
        Unit = runBlocking {
        TestSyncServer(directory).use { server ->
            val account = server.account("reader")
            SyncingTestDevice(directory.resolve("a.db")).use { a ->
                SyncingTestDevice(directory.resolve("b.db")).use { b ->
                    a.join(server, account)
                    a.sync() shouldBe SyncOutcome.SYNCED
                    b.join(server, account)
                    b.sync() shouldBe SyncOutcome.SYNCED
                    b.choiceChanges.syncArea(SettingsArea.READERS, on = false)

                    a.animationSpeed().set(250)
                    a.sync() shouldBe SyncOutcome.SYNCED
                    b.sync() shouldBe SyncOutcome.SYNCED
                    b.animationSpeed().get() shouldBe 500

                    // Changed after A's, yet the account's wins once B syncs the area again keeping the account's
                    b.animationSpeed().set(1)
                    b.sync() shouldBe SyncOutcome.SYNCED
                    a.sync() shouldBe SyncOutcome.SYNCED
                    a.animationSpeed().get() shouldBe 250

                    b.choiceChanges.asksWhoseSettings(SettingsArea.READERS) shouldBe true
                    b.choiceChanges.syncArea(SettingsArea.READERS, on = true)
                    b.animationSpeed().get() shouldBe 250
                    b.sync() shouldBe SyncOutcome.SYNCED
                    a.sync() shouldBe SyncOutcome.SYNCED
                    a.animationSpeed().get() shouldBe 250
                }
            }
        }
    }

    private fun SyncingTestDevice.animationSpeed() =
        local.preferences.profiles.profileStore(1).getInt("pref_double_tap_anim_speed", 500)
}
