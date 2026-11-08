package app.katari.sync.server.record

import app.katari.sync.protocol.error.SyncErrorCode
import app.katari.sync.server.TestServer
import app.katari.sync.server.device.AuthenticationOutcome
import app.katari.sync.server.record
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.listDirectoryEntries

class ReplacedServerTest {

    @Test
    fun `a data directory restored from an old copy or started over shows in the server's answers`(
        @TempDir root: Path,
    ) {
        val live = root.resolve("live").createDirectories()
        val backup = root.resolve("backup").createDirectories()
        lateinit var phoneToken: String
        lateinit var desktopToken: String
        val instanceId = TestServer(live).use { server ->
            val account = server.account()
            val phone = server.device(account)
            phoneToken = phone.token
            desktopToken = server.device(account).token
            phone.push(record("entry:1", basedOn = 0), record("entry:2", basedOn = 0))
            server.services.storage.instanceId
        }
        live.listDirectoryEntries().forEach { it.copyTo(backup.resolve(it.fileName)) }
        val phoneChain = TestServer(live).use { server ->
            val phone = server.LinkedTestDevice(phoneToken)
            phone.push(record("entry:3", basedOn = 0))
            val page = phone.pull(after = 0).shouldBeInstanceOf<PullOutcome.Page>().page
            page.nextCursor shouldBe 3
            phone.pull(after = 3, afterChain = page.nextChain).shouldBeInstanceOf<PullOutcome.Page>()
            page.nextChain
        }

        TestServer(backup).use { restored ->
            val phone = restored.LinkedTestDevice(phoneToken)
            restored.services.storage.instanceId shouldBe instanceId
            phone.pull(after = 3, afterChain = phoneChain).shouldBeInstanceOf<PullOutcome.Refused>()
                .code shouldBe SyncErrorCode.CURSOR_UNKNOWN

            // Another device pushes past the phone's old position before the phone comes back, so the sequence
            // alone no longer gives the restore away.
            val desktop = restored.LinkedTestDevice(desktopToken)
            desktop.push(record("entry:4", basedOn = 0), record("entry:5", basedOn = 0))
            restored.services.snapshot.of(phone.device.accountId).server.sequence shouldBe 4
            phone.pull(after = 3, afterChain = phoneChain).shouldBeInstanceOf<PullOutcome.Refused>()
                .code shouldBe SyncErrorCode.CURSOR_UNKNOWN
        }

        TestServer(root.resolve("empty").createDirectories()).use { replaced ->
            replaced.services.storage.instanceId shouldNotBe instanceId
            replaced.services.authentication.authenticate(
                phoneToken,
            ).shouldBeInstanceOf<AuthenticationOutcome.Refused>()
                .code shouldBe SyncErrorCode.UNAUTHORIZED
        }
    }
}
