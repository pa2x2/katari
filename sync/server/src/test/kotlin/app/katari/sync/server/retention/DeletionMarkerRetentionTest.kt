package app.katari.sync.server.retention

import app.katari.sync.protocol.error.SyncErrorCode
import app.katari.sync.protocol.push.PushOutcome
import app.katari.sync.server.TestServer
import app.katari.sync.server.record
import app.katari.sync.server.record.PullOutcome
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.time.Duration.Companion.days

class DeletionMarkerRetentionTest {

    @Test
    fun `markers are kept until the slowest active device passes them, and revoked or silent devices stop holding them`(
        @TempDir dataDirectory: Path,
    ) {
        TestServer(dataDirectory).use { server ->
            val account = server.account()
            val fast = server.device(account)
            val slow = server.device(account)
            val revoked = server.device(account)
            val silent = server.device(account)
            listOf(fast, slow, revoked, silent).forEach { it.pull(after = 0) }
            fast.push(record("entry:1", basedOn = 0))
            fast.push(record("entry:1", basedOn = 1, deleted = true))
            val retention = DeletionMarkerRetention(server.services.storage, server.clock)

            fast.pull(after = 2)
            retention.sweep()
            slow.markerIsAfter(0) shouldBe true

            slow.pull(after = 2)
            server.clock.advance(DeletionMarkerRetention.DEVICE_SILENCE_LIMIT + 1.days)
            fast.pull(after = 2)
            slow.pull(after = 2)
            revoked.pull(after = 0)
            retention.sweep()
            revoked.markerIsAfter(0) shouldBe true

            server.services.devices.revoke(account, revoked.device.id)
            retention.sweep()

            silent.pull(after = 0).shouldBeInstanceOf<PullOutcome.Refused>().code shouldBe SyncErrorCode.CURSOR_EXPIRED
            silent.pull(after = 0, liveOnly = true).shouldBeInstanceOf<PullOutcome.Page>()
            fast.pull(after = 2).shouldBeInstanceOf<PullOutcome.Page>()
        }
    }

    @Test
    fun `an edit based on a dropped marker is refused as stale against nothing, so the device merges it first`(
        @TempDir dataDirectory: Path,
    ) {
        TestServer(dataDirectory).use { server ->
            val account = server.account()
            val device = server.device(account)
            device.push(record("entry:1", basedOn = 0))
            device.push(record("entry:1", basedOn = 1, deleted = true))
            device.pull(after = 2)
            DeletionMarkerRetention(server.services.storage, server.clock).sweep()

            val onMarker = device.push(record("entry:1", basedOn = 2)).results.single()
            onMarker.outcome shouldBe PushOutcome.STALE
            onMarker.sequence shouldBe 0
            device.push(record("entry:1", basedOn = 0)).results.single().outcome shouldBe PushOutcome.ACCEPTED
        }
    }

    @Test
    fun `a device taking the whole account pages through records older than the history kept`(
        @TempDir dataDirectory: Path,
    ) {
        TestServer(dataDirectory).use { server ->
            val account = server.account()
            val old = server.device(account)
            old.push(record("entry:1", basedOn = 0), record("entry:2", basedOn = 0))
            old.push(record("entry:2", basedOn = 2))
            old.pull(after = 3)
            DeletionMarkerRetention(server.services.storage, server.clock).sweep()

            val new = server.device(account)
            val first = new.pull(after = 0, liveOnly = true, limit = 1).shouldBeInstanceOf<PullOutcome.Page>().page
            first.records.single().sequence shouldBe 1
            val second = new.pull(after = first.nextCursor, liveOnly = true, afterChain = first.nextChain, limit = 1)
                .shouldBeInstanceOf<PullOutcome.Page>().page
            second.records.single().sequence shouldBe 3
            second.hasMore shouldBe false
        }
    }

    private fun TestServer.LinkedTestDevice.markerIsAfter(position: Long): Boolean =
        pull(after = position).shouldBeInstanceOf<PullOutcome.Page>().page.records.any { it.record.deleted }
}
