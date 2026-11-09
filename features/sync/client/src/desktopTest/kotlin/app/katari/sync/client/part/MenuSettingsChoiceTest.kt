package app.katari.sync.client.part

import app.katari.sync.client.SyncingTestDevice
import app.katari.sync.client.TestSyncServer
import app.katari.sync.client.run.SyncOutcome
import app.katari.sync.client.setting.scope.MenuSettings
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class MenuSettingsChoiceTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `a menu kept out stays here while the rest of its area syncs, and synced again takes the account's`(): Unit =
        runBlocking {
            TestSyncServer(directory).use { server ->
                val account = server.account("reader")
                SyncingTestDevice(directory.resolve("a.db")).use { a ->
                    SyncingTestDevice(directory.resolve("b.db")).use { b ->
                        a.join(server, account)
                        a.sync() shouldBe SyncOutcome.SYNCED
                        b.join(server, account)
                        b.sync() shouldBe SyncOutcome.SYNCED
                        b.choiceChanges.syncMenu(MenuSettings.LIBRARY_FILTERS, on = false)

                        a.completedFilter().set("ENABLED_IS")
                        a.unreadBadge().set(false)
                        a.sync() shouldBe SyncOutcome.SYNCED
                        b.sync() shouldBe SyncOutcome.SYNCED
                        b.completedFilter().get() shouldBe "DISABLED"
                        b.unreadBadge().get() shouldBe false

                        b.choiceChanges.syncMenu(MenuSettings.LIBRARY_FILTERS, on = true)
                        b.completedFilter().get() shouldBe "ENABLED_IS"
                    }
                }
            }
        }

    private fun SyncingTestDevice.completedFilter() =
        local.preferences.profiles.profileStore(1).getString("pref_filter_library_completed_v2", "DISABLED")

    private fun SyncingTestDevice.unreadBadge() =
        local.preferences.profiles.profileStore(1).getBoolean("display_unread_badge", true)
}
