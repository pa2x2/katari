package eu.kanade.tachiyomi.data.backup.restore.restorers

import eu.kanade.tachiyomi.data.backup.models.BackupChapter
import eu.kanade.tachiyomi.data.backup.models.BackupHistory
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.history.model.History
import java.util.Date

class EntryRestorerTest {

    @Test
    fun `legacy manga page state converts to generic progress`() {
        val state = BackupChapter(
            url = "/chapter",
            name = "Chapter",
            lastPageRead = 4,
        ).toMangaProgressSnapshot(historyTimestamp = 2_000)

        state!!.resourceKey shouldBe "/chapter"
        state.locator.kind shouldBe "page"
        state.locator.position shouldBe 4L
        state.completed shouldBe false
        state.locatorUpdatedAt shouldBe 2_000L
    }

    @Test
    fun `untouched legacy manga chapter does not create progress`() {
        BackupChapter(
            url = "/chapter",
            name = "Chapter",
        ).toMangaProgressSnapshot(historyTimestamp = 0).shouldBeNull()
    }

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
