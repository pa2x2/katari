package app.katari.sync.server.device

import app.katari.sync.protocol.device.DeviceDescription
import app.katari.sync.protocol.device.RegisterDeviceRequest
import app.katari.sync.server.TestServer
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class DeviceMessageKindsTest {

    @Test
    fun `the account's device list shows the message kinds each device last said it acts on`(
        @TempDir dataDirectory: Path,
    ) {
        TestServer(dataDirectory).use { server ->
            val account = server.account()
            val code = server.services.devices.issueLinkCode(account, issuedBy = null).code
            val registered = server.services.registration.register(
                RegisterDeviceRequest(
                    DeviceDescription("Phone", "Android", "1", messageKinds = listOf("test", "download")),
                    linkCode = code,
                ),
                remoteAddress = "192.0.2.1",
            ) as RegistrationOutcome.Registered
            val older = server.device(account)
            fun kinds() = server.services.devices.list(account).devices.associate {
                it.description.name to it.description.messageKinds
            }

            kinds() shouldBe mapOf("Phone" to listOf("test", "download"), "Device" to emptyList())

            val phone = server.LinkedTestDevice(registered.response.token).device
            server.services.devices.describe(phone, phone.description.copy(messageKinds = listOf("download")))
            server.services.devices.describe(older.device, older.device.description.copy(messageKinds = listOf("test")))

            kinds() shouldBe mapOf("Phone" to listOf("download"), "Device" to listOf("test"))
        }
    }
}
