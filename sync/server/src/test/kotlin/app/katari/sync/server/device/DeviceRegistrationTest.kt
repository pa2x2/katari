package app.katari.sync.server.device

import app.katari.sync.protocol.device.DeviceDescription
import app.katari.sync.protocol.device.PasswordSignIn
import app.katari.sync.protocol.device.RegisterDeviceRequest
import app.katari.sync.server.TestServer
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.time.Duration.Companion.minutes

class DeviceRegistrationTest {

    @Test
    fun `failed passwords block the account from every address until the back-off passes`(
        @TempDir dataDirectory: Path,
    ) {
        TestServer(dataDirectory).use { server ->
            val passwords = server.services.passwords
            server.services.storage.accounts.create(
                "reader",
                passwords.hash("right password"),
                false,
                server.clock.millis,
            )
            fun signIn(password: String, from: String) = server.services.registration.register(
                RegisterDeviceRequest(DeviceDescription("Phone", "Android", "1"), PasswordSignIn("Reader", password)),
                remoteAddress = from,
            )

            repeat(6) { signIn("guess $it", from = "198.51.100.7") shouldBe RegistrationOutcome.Refused }

            signIn("right password", from = "198.51.100.7").shouldBeInstanceOf<RegistrationOutcome.Throttled>()
            signIn("right password", from = "203.0.113.9").shouldBeInstanceOf<RegistrationOutcome.Throttled>()
            server.clock.advance(1.minutes)
            signIn("right password", from = "203.0.113.9").shouldBeInstanceOf<RegistrationOutcome.Registered>()
        }
    }
}
