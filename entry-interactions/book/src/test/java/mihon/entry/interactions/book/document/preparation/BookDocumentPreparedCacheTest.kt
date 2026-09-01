package mihon.entry.interactions.book.document.preparation

import mihon.book.api.document.BookDocument
import mihon.book.api.document.BookDocumentBlock
import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentBlockId
import mihon.book.api.document.BookDocumentBlockKind
import mihon.book.api.document.BookDocumentBlockRole
import mihon.book.api.document.BookDocumentContent
import mihon.book.api.document.BookDocumentPublicationModel
import mihon.book.api.document.BookDocumentRichText
import mihon.book.api.document.BookDocumentTextRange
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class BookDocumentPreparedCacheTest {
    @Test
    fun `cache restores only an exact publication revision and model schema`() {
        val directory = Files.createTempDirectory("book-document-cache").toFile()
        val cache = BookDocumentPreparedCache(RuntimeEnvironment.getApplication(), directory)
        val key = BookDocumentPreparedCacheKey("publication", "exact-digest")
        val value = BookDocumentPreparedCacheValue(
            model = BookDocumentPublicationModel(listOf(document())),
            documentTitles = mapOf("chapter" to "Chapter"),
        )

        cache.write(key, value)

        assertEquals(value, cache.read(key))
        assertNull(cache.read(key.copy(revision = "different-digest")))
        assertNull(cache.read(key.copy(modelVersion = key.modelVersion + 1)))
        directory.deleteRecursively()
    }

    private fun document(): BookDocument {
        val text = "Readable text"
        return BookDocument(
            resourceId = "chapter",
            revision = "exact-digest",
            content = BookDocumentContent(
                text = text,
                blocks = listOf(
                    BookDocumentBlock(
                        id = BookDocumentBlockId("paragraph"),
                        role = BookDocumentBlockRole(BookDocumentBlockKind.PARAGRAPH),
                        content = BookDocumentBlockContent.Text(
                            BookDocumentRichText(text, BookDocumentTextRange(0, text.length)),
                        ),
                        plainText = text,
                        sourceFragments = emptyList(),
                        logicalStart = 0,
                        logicalEndExclusive = text.length,
                    ),
                ),
                anchors = emptyMap(),
            ),
        )
    }
}
