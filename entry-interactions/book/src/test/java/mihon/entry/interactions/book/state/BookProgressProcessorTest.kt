package mihon.entry.interactions.book

import eu.kanade.tachiyomi.source.entry.EntryType
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.EntryProgressResourceMapping
import mihon.entry.interactions.EntryProgressSnapshot
import mihon.entry.interactions.EntryProgressStateSnapshot
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressLocator
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.entry.model.progressResourceKey
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import kotlin.test.assertEquals

class BookProgressProcessorTest {
    @Test
    fun `migration maps source child identity into pending target progress`() = runTest {
        val source = entry(id = 1L)
        val target = entry(id = 2L)
        val sourceChapter = chapter(id = 11L, entryId = source.id, url = "/source/chapter")
        val targetChapter = chapter(id = 21L, entryId = target.id, url = "/target/chapter")
        val locator = EntryProgressLocator(kind = BOOK_PROGRESS_LOCATOR_KIND, progression = 0.4)
        val sourceState = EntryProgressState(
            entryId = source.id,
            chapterId = sourceChapter.id,
            contentKey = "source-publication",
            resourceKey = "source-media-resource",
            resourceRevision = "source-v1",
            locator = locator,
            locatorUpdatedAt = 50L,
        )
        val progressRepository = mockk<EntryProgressRepository> {
            coEvery { getByEntryId(source.id) } returns listOf(sourceState)
        }
        val chapterRepository = mockk<EntryChapterRepository> {
            coEvery { getChapterById(sourceChapter.id) } returns sourceChapter
        }
        val processor = BookProgressProcessor(progressRepository, chapterRepository)

        val prepared = processor.prepareMigration(
            source,
            target,
            listOf(
                EntryProgressResourceMapping(
                    sourceResourceKey = sourceChapter.progressResourceKey,
                    targetResourceKey = targetChapter.progressResourceKey,
                    targetChapterId = targetChapter.id,
                ),
            ),
        )

        assertEquals(
            EntryProgressStateSnapshot(
                contentKey = BOOK_PENDING_MIGRATION_CONTENT_KEY,
                resourceKey = bookPendingMigrationResourceKey(targetChapter.id),
                sourceChildKey = targetChapter.progressResourceKey,
                resourceRevision = null,
                locator = locator,
                locatorUpdatedAt = 50L,
            ),
            prepared.states.single(),
        )
    }

    @Test
    fun `restore resolves a blank chapter url through its durable progress key`() = runTest {
        val target = entry(id = 2L)
        val targetChapter = chapter(id = 21L, entryId = target.id, url = "")
        val restored = slot<EntryProgressState>()
        val progressRepository = mockk<EntryProgressRepository> {
            coEvery { mergeAndSyncChild(capture(restored)) } answers { restored.captured }
        }
        val chapterRepository = mockk<EntryChapterRepository> {
            coEvery { getChaptersByEntryIdAwait(target.id) } returns listOf(targetChapter)
        }
        val processor = BookProgressProcessor(progressRepository, chapterRepository)

        processor.restore(
            target,
            EntryProgressSnapshot(
                listOf(
                    EntryProgressStateSnapshot(
                        contentKey = BOOK_PENDING_MIGRATION_CONTENT_KEY,
                        resourceKey = bookPendingMigrationResourceKey(targetChapter.id),
                        sourceChildKey = targetChapter.progressResourceKey,
                        locator = EntryProgressLocator(kind = BOOK_PROGRESS_LOCATOR_KIND, progression = 0.2),
                    ),
                ),
            ),
        )

        assertEquals(targetChapter.id, restored.captured.chapterId)
    }

    private fun entry(id: Long): Entry = Entry.create().copy(
        id = id,
        source = id + 100,
        url = "/book-$id",
        title = "Book $id",
        type = EntryType.BOOK,
    )

    private fun chapter(
        id: Long,
        entryId: Long,
        url: String,
    ): EntryChapter = EntryChapter.create().copy(
        id = id,
        entryId = entryId,
        url = url,
        name = "Chapter",
    )
}
