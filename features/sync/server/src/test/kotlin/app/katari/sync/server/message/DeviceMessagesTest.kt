package app.katari.sync.server.message

import app.katari.sync.protocol.message.MessagePage
import app.katari.sync.server.TestServer
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

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
