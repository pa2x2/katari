package mihon.entry.interactions.book.document.reader

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.book.api.document.BookDocumentLinkTarget
import mihon.entry.interactions.book.document.preparation.preparedDocumentPublication
import mihon.entry.interactions.book.document.render.toPreparedBookDocument
import mihon.entry.interactions.book.navigation.BookChapterReadingOrder
import mihon.entry.interactions.book.reader.OpenedBookReaderSession
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.EntryChapter
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class BookDocumentLinkOwnershipTest {
    @Test
    fun `a reference in an adjacent chapter resolves in its owner and preserves the active chapter`() = runTest {
        val active = EntryChapter.create().copy(id = 1)
        val adjacent = EntryChapter.create().copy(id = 2)
        val activePublication = preparedDocumentPublication("text" to "<p id='note'>Wrong publication.</p>")
        val adjacentPublication = preparedDocumentPublication("text" to "<p id='note'>Correct adjacent note.</p>")
        val retained = BookDocumentReaderSessionViewModel()
        retained.attachInitial(
            mockk<OpenedBookReaderSession> {
                every { chapter } returns active
                every { preparedPublication } returns activePublication
                every { initialLocator } returns null
            },
        )
        retained.cache(
            mockk<OpenedBookReaderSession> {
                every { chapter } returns adjacent
                every { preparedPublication } returns adjacentPublication
                every { initialLocator } returns null
            },
        )
        val order = BookChapterReadingOrder(listOf(active, adjacent))
        var state = BookDocumentReaderState(
            entryTitle = "Book",
            readingOrder = order,
            currentChapterId = active.id,
            window = order.window(active.id)!!,
            loadedSections = emptyMap(),
        )
        var missing = 0
        val coordinator = BookDocumentChapterCoordinator(
            context = mockk(), scope = this, retainedSessions = retained, sessionFactory = mockk(),
            isNextChapterPreparationEnabled = { false }, currentState = { state }, updateState = { state = it },
            updateVisualChapterProgression = {}, onNavigationMissing = { missing++ }, onSessionActivated = {},
        )
        val document = adjacentPublication.documents.single()
        val source = BookDocumentSection(
            key = "adjacent",
            owner = adjacent,
            document = document.toPreparedBookDocument(),
            initialPosition = document.positionAtProgression(0f),
            resourceLoader = adjacentPublication.resourceLoader,
        )
        coordinator.navigateLink(source, BookDocumentLinkTarget.Reference(null, "note"))
        assertEquals(document, assertNotNull(state.auxiliarySection).document.document)
        assertEquals(adjacent.id, state.auxiliarySection?.owner?.id)
        assertEquals(active.id, state.currentChapterId)
        assertNull(retained.locator(active.id))
        coordinator.dismissAuxiliarySection()
        coordinator.navigateLink(source, BookDocumentLinkTarget.Resource("text", "missing"))
        assertEquals(1, missing)
        assertNull(state.navigationRequest)
        coordinator.close()
    }
}
