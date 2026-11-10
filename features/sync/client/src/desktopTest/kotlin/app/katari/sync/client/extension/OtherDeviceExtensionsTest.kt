package app.katari.sync.client.extension

import app.katari.sync.client.SyncingTestDevice
import app.katari.sync.client.TestSyncServer
import app.katari.sync.client.part.DataGroup
import app.katari.sync.client.run.SyncOutcome
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class OtherDeviceExtensionsTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `each device is offered the newest install or uninstall another device made`(): Unit = runBlocking {
        TestSyncServer(directory, pageSize = 500).use { server ->
            val account = server.account("reader")
            SyncingTestDevice(directory.resolve("a.db")).use { a ->
                SyncingTestDevice(directory.resolve("b.db")).use { b ->
                    // b had this one before it kept its installs
                    b.sql(install("old", installed = true, changedAt = 0))
                    a.join(server, account)
                    a.sync() shouldBe SyncOutcome.SYNCED
                    b.join(server, account)
                    b.sync() shouldBe SyncOutcome.SYNCED

                    a.sql(install("show", installed = true, changedAt = 1000))
                    a.sync() shouldBe SyncOutcome.SYNCED
                    b.sync() shouldBe SyncOutcome.SYNCED
                    a.sync() shouldBe SyncOutcome.SYNCED
                    a.offers() shouldBe listOf(OtherDeviceInstall("old", installed = true, "b", changedAt = 0))
                    b.offers() shouldBe listOf(OtherDeviceInstall("show", installed = true, "a", changedAt = 1000))

                    // b installing it too outweighs a's install there, and is what a is offered now
                    b.sql(install("show", installed = true, changedAt = 2000))
                    b.sync() shouldBe SyncOutcome.SYNCED
                    a.sync() shouldBe SyncOutcome.SYNCED
                    b.offers().shouldBeEmpty()
                    a.offers().single { it.pkgName == "show" } shouldBe
                        OtherDeviceInstall("show", installed = true, "b", changedAt = 2000)

                    // Both devices' shares stay in the record, so a's later uninstall reaches b
                    a.sql(install("show", installed = false, changedAt = 3000))
                    a.sync() shouldBe SyncOutcome.SYNCED
                    b.sync() shouldBe SyncOutcome.SYNCED
                    b.offers() shouldBe listOf(OtherDeviceInstall("show", installed = false, "a", changedAt = 3000))

                    b.preferences.dataOff.set(setOf(DataGroup.INSTALLED_EXTENSIONS.name))
                    b.offers().shouldBeEmpty()
                }
            }
        }
    }

    private fun install(pkgName: String, installed: Boolean, changedAt: Long) =
        """
        INSERT INTO extension_installs(pkg_name, installed, changed_at)
        VALUES ('$pkgName', ${if (installed) 1 else 0}, $changedAt)
        ON CONFLICT(pkg_name) DO UPDATE SET installed = excluded.installed, changed_at = excluded.changed_at
        """

    private suspend fun SyncingTestDevice.offers() = otherDeviceExtensions.subscribe().first()
}
