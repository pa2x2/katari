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
        lateinit var token: String
        val instanceId = TestServer(live).use { server ->
            val device = server.device(server.account())
            token = device.token
            device.push(record("entry:1", basedOn = 0), record("entry:2", basedOn = 0))
            server.services.storage.instanceId
        }
        live.listDirectoryEntries().forEach { it.copyTo(backup.resolve(it.fileName)) }
        TestServer(live).use { server ->
            val device = server.LinkedTestDevice(token)
            device.push(record("entry:3", basedOn = 0))
            device.pull(after = 0).shouldBeInstanceOf<PullOutcome.Page>().page.nextCursor shouldBe 3
        }

        TestServer(backup).use { restored ->
            val device = restored.LinkedTestDevice(token)
            restored.services.storage.instanceId shouldBe instanceId
            device.pull(after = 3).shouldBeInstanceOf<PullOutcome.Refused>().code shouldBe SyncErrorCode.CURSOR_UNKNOWN
            restored.services.snapshot.of(device.device.accountId).server.sequence shouldBe 2
        }

        TestServer(root.resolve("empty").createDirectories()).use { replaced ->
            replaced.services.storage.instanceId shouldNotBe instanceId
            replaced.services.authentication.authenticate(token).shouldBeInstanceOf<AuthenticationOutcome.Refused>()
                .code shouldBe SyncErrorCode.UNAUTHORIZED
        }
    }
}
