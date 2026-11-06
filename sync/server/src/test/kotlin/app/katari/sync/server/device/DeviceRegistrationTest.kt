package app.katari.sync.server.device

import app.katari.sync.protocol.device.DeviceDescription
import app.katari.sync.protocol.device.LinkCodeAccount
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

    @Test
    fun `looking a link code up leaves it to join with and its wrong guesses count toward joining's limit`(
        @TempDir dataDirectory: Path,
    ) {
        TestServer(dataDirectory).use { server ->
            val account = server.account("reader")
            val registration = server.services.registration
            fun join(code: String, from: String) = registration.register(
                RegisterDeviceRequest(DeviceDescription("Laptop", "Linux", "1"), linkCode = code),
                remoteAddress = from,
            )
            val code = server.services.devices.issueLinkCode(account).code

            repeat(2) {
                registration.accountOfLinkCode(code, "198.51.100.7") shouldBe
                    LinkCodeLookupOutcome.Found(LinkCodeAccount("reader"))
            }
            join(code, from = "198.51.100.7").shouldBeInstanceOf<RegistrationOutcome.Registered>()
            registration.accountOfLinkCode(code, "198.51.100.7") shouldBe LinkCodeLookupOutcome.Refused

            // With the used code, six failed guesses: the sixth blocks the address for joining as well
            val next = server.services.devices.issueLinkCode(account).code
            repeat(5) {
                registration.accountOfLinkCode("WRONG-$it", "198.51.100.7") shouldBe
                    LinkCodeLookupOutcome.Refused
            }
            join(next, from = "198.51.100.7").shouldBeInstanceOf<RegistrationOutcome.Throttled>()
            registration.accountOfLinkCode(next, "198.51.100.7").shouldBeInstanceOf<LinkCodeLookupOutcome.Throttled>()
            registration.accountOfLinkCode(next, "203.0.113.9") shouldBe
                LinkCodeLookupOutcome.Found(LinkCodeAccount("reader"))
        }
    }
}
