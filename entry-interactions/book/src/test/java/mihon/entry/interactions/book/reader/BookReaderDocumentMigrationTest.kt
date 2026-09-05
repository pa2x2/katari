package mihon.entry.interactions.book.reader

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.book.api.BookLocator
import mihon.book.api.BookTextContext
import mihon.book.api.document.locatorAt
import mihon.book.api.document.resolvePosition
import mihon.entry.interactions.book.document.preparation.preparedDocumentPublication
import mihon.entry.interactions.book.migration.BOOK_PENDING_MIGRATION_CONTENT_KEY
import mihon.entry.interactions.book.state.BookProgressIdentity
import mihon.entry.interactions.book.state.BookProgressLocatorCodec
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.entry.repository.EntryProgressRepository
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class BookReaderDocumentMigrationTest : BookReaderSessionFixture() {
    @Test
    fun `canonical reader restores renamed HTML by passage instead of foreign block identity`() = runTest {
        val original =
            preparedDocumentPublication("old.html" to "<p>Opening.</p><p>A distinctive passage to resume.</p>")
        val originalDocument = original.documents.single()
        val position = originalDocument.resolvePosition(
            BookLocator("old.html", textContext = BookTextContext(after = "A distinctive passage")),
        )!!
        val locator = originalDocument.locatorAt(position)
        val target = preparedDocumentPublication(
            "new.html" to "<p>Added introduction.</p><p>Opening.</p><p>A distinctive passage to resume.</p>",
        )
        val session = openWithProgress(chapter(), bookProgress(locator, false), target)
        val restored = assertNotNull(session.initialLocator)
        val document = target.documents.single()
        val restoredPosition = assertNotNull(document.resolvePosition(restored))
        assertEquals(document.blocks.last().id, restoredPosition.blockId)
        assertEquals(0, restoredPosition.offsetWithinBlock)
        session.close()
    }

    @Test
    fun `ambiguous migrated passage remains pending without destroying saved evidence`() = runTest {
        val publication = preparedDocumentPublication(
            "one.html" to "<p>The repeated passage.</p>",
            "two.html" to "<p>The repeated passage.</p>",
        )
        val locator = BookLocator("old.html", progression = 0.5, textContext = BookTextContext(after = "The repeated"))
        val pending = EntryProgressState(
            entryId = entry().id,
            chapterId = chapter().id,
            contentKey = BOOK_PENDING_MIGRATION_CONTENT_KEY,
            resourceKey = "target-child:${chapter().id}",
            locator = BookProgressLocatorCodec.encode(locator),
            locatorUpdatedAt = 50L,
        )
        val repository = mockk<EntryProgressRepository> {
            coEvery { get(any(), any(), any()) } returns null
            coEvery { getByEntryId(entry().id) } returns listOf(pending)
        }
        val result = BookReaderProgressResolver(repository).resolve(
            chapter(),
            BookProgressIdentity("volume-1", "chapter.html", null),
            publication,
        )
        assertNull(result)
        coVerify(exactly = 0) { repository.upsert(any()) }
        coVerify(exactly = 0) { repository.rekey(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `fraction-only migration requires an identifiable document`() = runTest {
        val single = preparedDocumentPublication("new.html" to "<p>A chapter of ordinary prose.</p>")
        assertNotNull(single.reconcileMigratedLocator(BookLocator("old.html", progression = 0.5)))
        val multiple = preparedDocumentPublication("one" to "<p>One.</p>", "two" to "<p>Two.</p>")
        assertNull(multiple.reconcileMigratedLocator(BookLocator("old.html", progression = 0.5)))
    }
}
