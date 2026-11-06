package app.katari.sync.server.invite

import app.katari.sync.protocol.account.RedeemInviteRequest
import app.katari.sync.protocol.device.DeviceDescription
import app.katari.sync.protocol.push.PushOutcome
import app.katari.sync.server.TestServer
import app.katari.sync.server.record
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.time.Duration.Companion.days

class InvitesTest {

    @Test
    fun `an invite makes one account with a syncing device, outlives a taken username, and expires after a day`(
        @TempDir dataDirectory: Path,
    ) {
        TestServer(dataDirectory).use { server ->
            val admin = server.account("admin", isAdmin = true)
            server.account("anna")
            val invites = server.services.invites
            fun redeem(code: String, username: String) = invites.redeem(
                RedeemInviteRequest(DeviceDescription("Phone", "Android", "1"), code, username, "a long password"),
                remoteAddress = "192.0.2.1",
            )
            val code = checkNotNull(invites.issue(admin))

            redeem(code, "Anna") shouldBe RedemptionOutcome.UsernameTaken
            val registered = redeem(code, "sam").shouldBeInstanceOf<RedemptionOutcome.Registered>()
            redeem(code, "sam2") shouldBe RedemptionOutcome.Refused

            val device = server.LinkedTestDevice(registered.response.token)
            server.services.storage.accounts.byId(device.device.accountId)?.username shouldBe "sam"
            device.push(record("entry:1", basedOn = 0)).results.single().outcome shouldBe PushOutcome.ACCEPTED

            val late = checkNotNull(invites.issue(admin))
            server.clock.advance(1.days)
            redeem(late, "max") shouldBe RedemptionOutcome.Refused
            server.services.storage.accounts.byUsername("max").shouldBeNull()
        }
    }
}
