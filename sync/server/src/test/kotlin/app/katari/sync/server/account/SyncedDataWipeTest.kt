package app.katari.sync.server.account

import app.katari.sync.protocol.error.SyncErrorCode
import app.katari.sync.protocol.push.PushOutcome
import app.katari.sync.server.TestServer
import app.katari.sync.server.device.AuthenticationOutcome
import app.katari.sync.server.record
import app.katari.sync.server.record.PullOutcome
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.exists

class SyncedDataWipeTest {

    @Test
    fun `wiping leaves the account empty and usable, and refuses every device and position from before`(
        @TempDir dataDirectory: Path,
    ) {
        TestServer(dataDirectory).use { server ->
            val account = server.account("anna", password = "anna's password")
            val phone = server.device(account)
            val laptop = server.device(account)
            val cover = server.upload(account, "custom cover")
            phone.push(record("entry:1", basedOn = 0).copy(files = listOf(cover)), record("entry:2", basedOn = 0))
            val oldPosition = (laptop.pull(after = 0) as PullOutcome.Page).page

            val anna = checkNotNull(server.services.storage.accounts.byId(account))
            server.services.dataWipe.wipe(anna, "anna's password", remoteAddress = "192.0.2.1") shouldBe
                WipeOutcome.Wiped

            listOf(phone, laptop).forEach {
                server.services.authentication.authenticate(it.token) shouldBe
                    AuthenticationOutcome.Refused(SyncErrorCode.DEVICE_REVOKED)
            }
            server.services.files.download(account, cover.category, cover.sha256).shouldBeNull()
            dataDirectory.resolve("files/$account").exists() shouldBe false
            server.services.storage.accounts.summary(account)?.storedBytes shouldBe 0

            val tablet = server.device(account)
            tablet.pull(after = oldPosition.nextCursor, afterChain = oldPosition.nextChain) shouldBe
                PullOutcome.Refused(SyncErrorCode.CURSOR_EXPIRED)
            val fresh = tablet.pull(after = 0, liveOnly = true).shouldBeInstanceOf<PullOutcome.Page>().page
            fresh.records.shouldBeEmpty()
            tablet.push(record("entry:1", basedOn = 0)).results.single().outcome shouldBe PushOutcome.ACCEPTED
            tablet.pull(after = fresh.nextCursor, afterChain = fresh.nextChain).shouldBeInstanceOf<PullOutcome.Page>()
                .page.records.map { it.record.key } shouldBe listOf("entry:1")
        }
    }
}
