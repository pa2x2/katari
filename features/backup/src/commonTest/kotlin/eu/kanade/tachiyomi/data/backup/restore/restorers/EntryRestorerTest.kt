package eu.kanade.tachiyomi.data.backup.restore.restorers

import eu.kanade.tachiyomi.data.backup.models.BackupChapter
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.data.backup.models.BackupHistory
import eu.kanade.tachiyomi.source.entry.ChapterKind
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.history.model.History
import java.util.Date

class EntryRestorerTest {

    @Test
    fun `history merge keeps newer timestamp, only adds missing duration and is idempotent`() {
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

        val repeated = BackupHistory(
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

        repeated.readAt shouldBe Date(2_000)
        repeated.sessionReadDuration shouldBe 0
    }

    @Test
    fun `restore keeps fetched details and chapter metadata, takes the backup's notes, reads new kinds as regular`() {
        val backup = BackupEntry(
            source = 1,
            url = "/entry",
            description = "Backed up",
            notes = "Mine",
            initialized = true,
        ).toEntry()
        val browsed = Entry.create().copy(id = 7, backgroundUrl = "/art.jpg")
        backup.restoredOnto(browsed).run {
            description shouldBe "Backed up"
            backgroundUrl shouldBe "/art.jpg"
            notes shouldBe "Mine"
            favorite shouldBe true
        }
        backup.restoredOnto(browsed.copy(description = "Fetched", initialized = true)).run {
            description shouldBe "Fetched"
            notes shouldBe "Mine"
        }

        val dbChapter = EntryChapter.create().copy(
            id = 3,
            entryId = 7,
            url = "/stored",
            name = "Episode 1",
            summary = "Recap",
            previewUrl = "/still.jpg",
            kind = ChapterKind.FILLER,
        )
        val restored = restoredChapters(
            entryId = 7,
            backupChapters = listOf(
                BackupChapter(url = "/stored", name = "Episode 1", read = true),
                BackupChapter(url = "/new", name = "Episode 2", kind = 99),
            ),
            dbChapters = listOf(dbChapter),
        ).associateBy { it.url }

        restored.getValue("/stored").run {
            read shouldBe true
            summary shouldBe "Recap"
            previewUrl shouldBe "/still.jpg"
            kind shouldBe ChapterKind.FILLER
        }
        restored.getValue("/new").kind shouldBe ChapterKind.REGULAR
    }
}
