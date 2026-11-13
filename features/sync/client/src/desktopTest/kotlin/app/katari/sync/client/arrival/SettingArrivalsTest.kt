package app.katari.sync.client.arrival

import app.katari.sync.client.SyncingTestDevice
import app.katari.sync.client.TestSyncServer
import app.katari.sync.client.part.ChoiceSet
import app.katari.sync.client.run.SyncOutcome
import app.katari.sync.client.setting.scope.MenuSettings
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class SettingArrivalsTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `a setting another device changed is listed, and Keep mine puts this device's value back`(): Unit =
        runBlocking {
            synced { a, b ->
                a.animationSpeed().set(1)
                a.sync() shouldBe SyncOutcome.SYNCED
                b.sync() shouldBe SyncOutcome.SYNCED
                a.sync() shouldBe SyncOutcome.SYNCED
                a.arrived(ANIMATION_SPEED).shouldBeEmpty()

                b.animationSpeed().set(750)
                b.sync() shouldBe SyncOutcome.SYNCED
                a.sync() shouldBe SyncOutcome.SYNCED
                a.animationSpeed().get() shouldBe 750
                val arrived = a.arrived(ANIMATION_SPEED).single()
                arrived.keepMine shouldBe KeepMine.OFFERED

                a.arrivals.keepMine(arrived)
                a.animationSpeed().get() shouldBe 1
                a.preferences.choices().settingsOff shouldBe setOf(ANIMATION_SPEED)
                a.arrived(ANIMATION_SPEED).single().keepMine shouldBe KeepMine.KEPT_HERE
                a.sync() shouldBe SyncOutcome.SYNCED
                b.sync() shouldBe SyncOutcome.SYNCED
                b.animationSpeed().get() shouldBe 750
            }
        }

    @Test
    fun `Keep mine on a menu's setting keeps the rest of the menu its sync changed`(): Unit = runBlocking {
        synced { a, b ->
            b.filter(DOWNLOADED).set(1)
            b.filter(UNREAD).set(2)
            b.sync() shouldBe SyncOutcome.SYNCED
            a.sync() shouldBe SyncOutcome.SYNCED
            val arrived = a.arrivals.changes().first().filter { it.menu == MenuSettings.LIBRARY_FILTERS }
            arrived.map { it.key }.toSet() shouldBe setOf(DOWNLOADED, UNREAD)

            a.arrivals.keepMine(arrived.first())
            a.filter(DOWNLOADED).get() shouldBe 0
            a.filter(UNREAD).get() shouldBe 0
            a.preferences.choices() shouldBe ChoiceSet(menusOff = setOf(MenuSettings.LIBRARY_FILTERS))
            (a.arrived(DOWNLOADED) + a.arrived(UNREAD)).map { it.keepMine }.toSet() shouldBe setOf(KeepMine.KEPT_HERE)
        }
    }

    @Test
    fun `a kept setting that syncs again no longer says it is kept here`(): Unit = runBlocking {
        synced { a, b ->
            b.animationSpeed().set(750)
            b.sync() shouldBe SyncOutcome.SYNCED
            a.sync() shouldBe SyncOutcome.SYNCED
            a.arrivals.keepMine(a.arrived(ANIMATION_SPEED).single())

            a.choiceChanges.syncSetting(ANIMATION_SPEED, on = true)
            a.arrived(ANIMATION_SPEED).single().keepMine shouldBe KeepMine.SYNCED_AGAIN
            a.choiceChanges.syncSetting(ANIMATION_SPEED, on = false)
            a.arrived(ANIMATION_SPEED).single().keepMine shouldBe KeepMine.KEPT_HERE
        }
    }

    private suspend fun synced(test: suspend (SyncingTestDevice, SyncingTestDevice) -> Unit) {
        TestSyncServer(directory).use { server ->
            val account = server.account("reader")
            SyncingTestDevice(directory.resolve("a.db")).use { a ->
                SyncingTestDevice(directory.resolve("b.db")).use { b ->
                    a.join(server, account)
                    a.sync() shouldBe SyncOutcome.SYNCED
                    b.join(server, account)
                    b.sync() shouldBe SyncOutcome.SYNCED
                    a.sync() shouldBe SyncOutcome.SYNCED
                    test(a, b)
                }
            }
        }
    }

    private suspend fun SyncingTestDevice.arrived(key: String) = arrivals.changes().first().filter { it.key == key }

    private fun SyncingTestDevice.animationSpeed() =
        local.preferences.profiles.profileStore(1).getInt(ANIMATION_SPEED, 500)

    private fun SyncingTestDevice.filter(key: String) = local.preferences.profiles.profileStore(1).getInt(key, 0)

    private companion object {
        const val ANIMATION_SPEED = "pref_double_tap_anim_speed"
        const val DOWNLOADED = "pref_filter_library_downloaded_v2"
        const val UNREAD = "pref_filter_library_unread_v2"
    }
}
