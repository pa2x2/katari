package app.katari.sync.server.record

import app.katari.sync.protocol.push.PushOutcome
import app.katari.sync.protocol.push.PushResult
import app.katari.sync.server.TestServer
import app.katari.sync.server.record
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class RecordPushTest {

    @Test
    fun `a push on an outdated copy is refused, one on the stored copy is accepted, and a retry is not applied twice`(
        @TempDir dataDirectory: Path,
    ) {
        TestServer(dataDirectory).use { server ->
            val account = server.account()
            val phone = server.device(account)
            val desktop = server.device(account)
            val created = record("entry:1", basedOn = 0, payload = "from phone", changeId = "phone-1")

            phone.push(created).results shouldContainExactly listOf(PushResult("phone-1", PushOutcome.ACCEPTED, 1))
            desktop.push(record("entry:1", basedOn = 0, changeId = "desktop-1")).results shouldContainExactly
                listOf(PushResult("desktop-1", PushOutcome.STALE, 1))
            desktop.push(record("entry:1", basedOn = 1, payload = "merged on desktop", changeId = "desktop-2")).results
                .shouldContainExactly(PushResult("desktop-2", PushOutcome.ACCEPTED, 2))

            val retried = phone.push(created)
            retried.results shouldContainExactly listOf(PushResult("phone-1", PushOutcome.ACCEPTED, 1))
            retried.server.sequence shouldBe 2

            val page = phone.pull(after = 0).shouldBeInstanceOf<PullOutcome.Page>().page
            page.records.map { it.sequence to it.record.payload.decodeToString() } shouldContainExactly
                listOf(2L to "merged on desktop")
            page.records.single().record.device shouldBe desktop.device.id
        }
    }

    @Test
    fun `a record later in a push is checked against the records written before it in the same push`(
        @TempDir dataDirectory: Path,
    ) {
        TestServer(dataDirectory).use { server ->
            val account = server.account()
            val phone = server.device(account)
            val cover = server.upload(account, "cover")
            val created = record("entry:1", basedOn = 0, changeId = "created").copy(files = listOf(cover))
            val edited = record("entry:1", basedOn = 1, payload = "edited", changeId = "edited")

            phone.push(created, edited, created).results shouldContainExactly listOf(
                PushResult("created", PushOutcome.ACCEPTED, 1),
                PushResult("edited", PushOutcome.ACCEPTED, 2),
                PushResult("created", PushOutcome.ACCEPTED, 1),
            )

            val page = phone.pull(after = 0).shouldBeInstanceOf<PullOutcome.Page>().page
            page.records.map { it.sequence to it.record.payload.decodeToString() } shouldContainExactly
                listOf(2L to "edited")
            page.records.single().record.files shouldBe emptyList()
        }
    }

    @Test
    fun `a push is answered as all the device missed only when nothing else was written since its position`(
        @TempDir dataDirectory: Path,
    ) {
        TestServer(dataDirectory).use { server ->
            val account = server.account()
            val phone = server.device(account)
            val desktop = server.device(account)

            phone.push(record("entry:1", basedOn = 0), after = 0).caughtUp shouldBe true
            // The phone is at 1; the desktop, still at 0, would skip the phone's record if it moved to 2
            desktop.push(record("entry:2", basedOn = 0), after = 0).caughtUp shouldBe false
            phone.push(record("entry:3", basedOn = 0), after = 1).caughtUp shouldBe false
            desktop.push(record("entry:4", basedOn = 0), after = 3).caughtUp shouldBe true
        }
    }
}
