package app.katari.entry.interactions.book.document.preparation

import app.katari.book.api.document.BookDocument
import app.katari.book.api.document.BookDocumentBlock
import app.katari.book.api.document.BookDocumentBlockContent
import app.katari.book.api.document.BookDocumentBlockId
import app.katari.book.api.document.BookDocumentBlockKind
import app.katari.book.api.document.BookDocumentBlockRole
import app.katari.book.api.document.BookDocumentContent
import app.katari.book.api.document.BookDocumentPublicationModel
import app.katari.book.api.document.BookDocumentRichText
import app.katari.book.api.document.BookDocumentTextRange
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
        val cache = BookDocumentPreparedCache(directory)
        val key = BookDocumentPreparedCacheKey("publication", "exact-digest")
        val value = BookDocumentPreparedCacheValue(
            model = BookDocumentPublicationModel(listOf(cachedDocument())),
            documentTitles = mapOf("chapter" to "Chapter"),
        )

        cache.write(key, value)

        assertEquals(value, cache.read(key))
        assertNull(cache.read(key.copy(revision = "different-digest")))
        assertNull(cache.read(key.copy(modelVersion = key.modelVersion + 1)))
        directory.deleteRecursively()
    }

    private fun cachedDocument(text: String = "Readable text"): BookDocument = BookDocument(
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
