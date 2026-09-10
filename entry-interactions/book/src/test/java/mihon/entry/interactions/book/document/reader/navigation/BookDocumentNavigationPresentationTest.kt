package mihon.entry.interactions.book.document.reader.navigation

import mihon.book.api.BookLocator
import mihon.book.api.BookNavigationItem
import mihon.book.api.document.locatorAt
import mihon.entry.interactions.book.document.preparation.preparedDocumentPublication
import mihon.entry.interactions.book.document.reader.BookDocumentPublicationSections
import mihon.entry.interactions.book.document.reader.BookDocumentReaderState
import mihon.entry.interactions.book.document.reader.BookDocumentSection
import mihon.entry.interactions.book.document.render.toPreparedBookDocument
import mihon.entry.interactions.book.navigation.BookChapterReadingOrder
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.EntryChapter
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class BookDocumentNavigationPresentationTest {
    @Test
    fun `one document with one root retains nested contents alongside other source volumes`() {
        val first = EntryChapter.create().copy(id = 1, name = "Volume one", bookmark = true)
        val second = EntryChapter.create().copy(id = 2, name = "Volume two")
        val order = BookChapterReadingOrder(listOf(first, second))
        val publication =
            preparedDocumentPublication("text" to "<h1 id='a'>First</h1><h2 id='b'>Second</h2><p>Prose.</p>")
        val document = publication.documents.single()
        val section = BookDocumentSection(
            key = "1:text",
            owner = first,
            document = document.toPreparedBookDocument(),
            initialPosition = document.positionAtProgression(0f),
            resourceLoader = publication.resourceLoader,
        )
        val navigation = listOf(
            BookNavigationItem(
                "First",
                BookLocator("text", fragments = listOf("a")),
                listOf(
                    BookNavigationItem("Second", BookLocator("text", fragments = listOf("b")), emptyList()),
                ),
            ),
        )
        val state = BookDocumentReaderState(
            entryTitle = "Book",
            readingOrder = order,
            currentChapterId = first.id,
            window = order.window(first.id)!!,
            loadedSections = mapOf(first.id to BookDocumentPublicationSections(listOf(section), section.key)),
            publicationNavigation = mapOf(first.id to navigation),
            navigationLocator = document.locatorAt(document.positionAtProgression(0.9f)),
        )
        val presented = state.documentNavigationPresentation()
        assertEquals(listOf("Volume one", "First", "Second", "Volume two"), presented.rows.map { it.title })
        assertEquals(listOf(0, 1, 2, 0), presented.rows.map { it.depth })
        assertEquals(2, presented.selectedIndex)
        assertNull(presented.rows[2].read)
        assertEquals(true, presented.rows[0].bookmark)
        assertEquals(second, presented.rows.last().item.chapter)
    }

    @Test
    fun `single chapter-start item does not duplicate its chapter row`() {
        val first = EntryChapter.create().copy(id = 1, name = "Chapter 1128: Contradiction")
        val second = EntryChapter.create().copy(id = 2, name = "Chapter 1129: Thieves and Wolves")
        val order = BookChapterReadingOrder(listOf(first, second))
        val publication =
            preparedDocumentPublication("text" to "<p>Prose.</p>")
        val document = publication.documents.single()
        val section = BookDocumentSection(
            key = "1:text",
            owner = first,
            document = document.toPreparedBookDocument(),
            initialPosition = document.positionAtProgression(0f),
            resourceLoader = publication.resourceLoader,
        )
        val redundant = listOf(
            BookNavigationItem(
                "Chapter 1128: Contradiction",
                BookLocator("text", progression = 0.0),
                emptyList(),
            ),
        )
        val state = BookDocumentReaderState(
            entryTitle = "Book",
            readingOrder = order,
            currentChapterId = first.id,
            window = order.window(first.id)!!,
            loadedSections = mapOf(first.id to BookDocumentPublicationSections(listOf(section), section.key)),
            publicationNavigation = mapOf(first.id to redundant, second.id to redundant),
            navigationLocator = document.locatorAt(document.positionAtProgression(0f)),
        )
        val presented = state.documentNavigationPresentation()
        assertEquals(
            listOf("Chapter 1128: Contradiction", "Chapter 1129: Thieves and Wolves"),
            presented.rows.map { it.title },
        )
        assertEquals(listOf(0, 0), presented.rows.map { it.depth })
        assertEquals(0, presented.selectedIndex)
    }

    @Test
    fun `single internal anchor is retained as nested navigation`() {
        val first = EntryChapter.create().copy(id = 1, name = "Chapter one")
        val order = BookChapterReadingOrder(listOf(first))
        val publication =
            preparedDocumentPublication("text" to "<h1 id='a'>Heading</h1><p>Prose.</p>")
        val document = publication.documents.single()
        val section = BookDocumentSection(
            key = "1:text",
            owner = first,
            document = document.toPreparedBookDocument(),
            initialPosition = document.positionAtProgression(0f),
            resourceLoader = publication.resourceLoader,
        )
        val navigation = listOf(
            BookNavigationItem(
                "Heading",
                BookLocator("text", fragments = listOf("a")),
                emptyList(),
            ),
        )
        val state = BookDocumentReaderState(
            entryTitle = "Book",
            readingOrder = order,
            currentChapterId = first.id,
            window = order.window(first.id)!!,
            loadedSections = mapOf(first.id to BookDocumentPublicationSections(listOf(section), section.key)),
            publicationNavigation = mapOf(first.id to navigation),
            navigationLocator = document.locatorAt(document.positionAtProgression(0f)),
        )
        val presented = state.documentNavigationPresentation()
        assertEquals(listOf("Chapter one", "Heading"), presented.rows.map { it.title })
        assertEquals(listOf(0, 1), presented.rows.map { it.depth })
    }
}
