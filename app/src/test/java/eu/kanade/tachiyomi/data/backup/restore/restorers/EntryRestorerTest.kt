package eu.kanade.tachiyomi.data.backup.restore.restorers

import eu.kanade.tachiyomi.data.backup.models.BackupHistory
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.history.model.History
import java.util.Date

class EntryRestorerTest {

    @Test
    fun `history merge keeps newer timestamp and only adds missing duration`() {
        val update = BackupHistory(
            url = "chapter",
            lastRead = 1_000,
            readDuration = 150,
        ).mergeWith(
            chapterId = 42,
            existingHistory = History(
                id = 1,
                chapterId = 42,
                readAt = Date(2_000),
                readDuration = 100,
            ),
        )

        update.chapterId shouldBe 42
        update.readAt shouldBe Date(2_000)
        update.sessionReadDuration shouldBe 50
    }

    @Test
    fun `restoring the same history is idempotent`() {
        val update = BackupHistory(
            url = "chapter",
            lastRead = 2_000,
            readDuration = 100,
        ).mergeWith(
            chapterId = 42,
            existingHistory = History(
                id = 1,
                chapterId = 42,
                readAt = Date(2_000),
                readDuration = 100,
            ),
        )

        update.readAt shouldBe Date(2_000)
        update.sessionReadDuration shouldBe 0
    }
}
