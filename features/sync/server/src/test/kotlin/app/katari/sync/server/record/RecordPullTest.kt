package app.katari.sync.server.record

import app.katari.sync.server.TestServer
import app.katari.sync.server.record
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class RecordPullTest {

    @Test
    fun `a download that leaves deletions out still takes those made after it began, which an earlier page may need`(
        @TempDir dataDirectory: Path,
    ) {
        TestServer(dataDirectory).use { server ->
            val account = server.account()
            val old = server.device(account)
            old.push(record("entry:1", basedOn = 0), record("entry:2", basedOn = 0), record("entry:3", basedOn = 0))
            old.push(record("entry:3", basedOn = 3, deleted = true))

            val new = server.device(account)
            val first = new.pull(after = 0, liveOnly = true, limit = 1).shouldBeInstanceOf<PullOutcome.Page>().page
            first.records.single().record.key shouldBe "entry:1"
            old.push(record("entry:1", basedOn = 1, deleted = true))

            val rest = new.pull(after = first.nextCursor, liveOnly = true, afterChain = first.nextChain)
                .shouldBeInstanceOf<PullOutcome.Page>().page
            rest.records.map { it.record.key to it.record.deleted } shouldContainExactly listOf(
                "entry:2" to false,
                "entry:1" to true,
            )
        }
    }
}
