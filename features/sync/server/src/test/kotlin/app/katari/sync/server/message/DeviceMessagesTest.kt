package app.katari.sync.server.message

import app.katari.sync.protocol.message.MessagePage
import app.katari.sync.server.SettableClock
import app.katari.sync.server.TestServer
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.listDirectoryEntries
import kotlin.time.Duration.Companion.minutes

class DeviceMessagesTest {

    @Test
    fun `a message reaches only the other linked devices of the sender's own account`(@TempDir dataDirectory: Path) {
        TestServer(dataDirectory).use { server ->
            val account = server.account()
            val phone = server.device(account)
            val desktop = server.device(account)
            val removed = server.device(account).device.id
            val stranger = server.device(server.account(username = "stranger"))
            server.services.devices.revoke(account, removed)

            phone.send("to all")?.recipients?.map { it.device } shouldContainExactly listOf(desktop.device.id)
            phone.send("to a stranger", to = stranger) shouldBe null
            phone.send("to a removed device", to = removed) shouldBe null

            stranger.fetch(after = 0).messages shouldBe emptyList()
            desktop.fetch(after = 0).texts() shouldContainExactly listOf("to all")
        }
    }

    @Test
    fun `a message sent after every earlier one was confirmed gets a number past them, so the device still takes it`(
        @TempDir dataDirectory: Path,
    ) {
        TestServer(dataDirectory).use { server ->
            val account = server.account()
            val phone = server.device(account)
            val desktop = server.device(account)
            phone.send("first", to = desktop)
            phone.send("second", to = desktop)
            val handled = desktop.fetch(after = 0).messages.last().number

            desktop.fetch(after = handled).messages shouldBe emptyList()
            phone.send("third", to = desktop)

            desktop.fetch(after = handled).texts() shouldContainExactly listOf("third")
        }
    }

    @Test
    fun `after the server is restored from a backup, a new message gets a number past those the device handled since`(
        @TempDir root: Path,
    ) {
        val live = root.resolve("live").createDirectories()
        val backup = root.resolve("backup").createDirectories()
        val clock = SettableClock()
        lateinit var phoneToken: String
        lateinit var desktopToken: String
        TestServer(live, clock).use { server ->
            val account = server.account()
            val phone = server.device(account)
            val desktop = server.device(account)
            phoneToken = phone.token
            desktopToken = desktop.token
            phone.send("before the backup", to = desktop)
        }
        live.listDirectoryEntries().forEach { it.copyTo(backup.resolve(it.fileName)) }
        clock.advance(1.minutes)
        val handled = TestServer(live, clock).use { server ->
            val phone = server.LinkedTestDevice(phoneToken)
            val desktop = server.LinkedTestDevice(desktopToken)
            phone.send("after the backup", to = desktop)
            desktop.fetch(after = 0).messages.last().number.also { desktop.fetch(after = it) }
        }

        clock.advance(1.minutes)
        TestServer(backup, clock).use { restored ->
            val phone = restored.LinkedTestDevice(phoneToken)
            val desktop = restored.LinkedTestDevice(desktopToken)
            phone.send("after the restore", to = desktop)

            desktop.fetch(after = handled).texts() shouldContainExactly listOf("after the restore")
        }
    }

    @Test
    fun `a message replaces only its own sender's waiting one, and a full queue drops its oldest`(
        @TempDir dataDirectory: Path,
    ) {
        TestServer(dataDirectory).use { server ->
            val account = server.account()
            val phone = server.device(account)
            val tablet = server.device(account)
            val desktop = server.device(account)
            phone.send("phone playing 1", to = desktop, replaces = "playing")
            tablet.send("tablet playing", to = desktop, replaces = "playing")
            phone.send("phone playing 2", to = desktop, replaces = "playing")

            desktop.fetch(after = 0).texts() shouldContainExactly listOf("tablet playing", "phone playing 2")

            repeat(DeviceMessages.MAX_WAITING.toInt() - 2) { phone.send("filler $it", to = desktop) }
            phone.send("one too many", to = desktop)

            desktop.fetch(after = 0).texts().first() shouldBe "phone playing 2"
        }
    }

    private fun MessagePage.texts() = messages.map { it.body.payload.decodeToString() }
}
