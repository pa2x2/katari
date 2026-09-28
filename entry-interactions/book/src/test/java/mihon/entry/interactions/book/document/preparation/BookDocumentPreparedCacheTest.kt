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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class BookDocumentPreparedCacheTest {
    @Test
    fun `cache restores only an exact publication revision and model schema`() {
        val directory = Files.createTempDirectory("book-document-cache").toFile()
        val cache = BookDocumentPreparedCache(RuntimeEnvironment.getApplication(), directory)
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

    @Test
    fun `oversized entries are skipped without displacing cached books or leaving partial files`() {
        val directory = Files.createTempDirectory("book-document-bounded-cache").toFile()
        try {
            val cache = BookDocumentPreparedCache(
                RuntimeEnvironment.getApplication(),
                directory,
                maxEntryBytes = 4096,
            )
            val key = BookDocumentPreparedCacheKey("publication", "revision")
            val small =
                BookDocumentPreparedCacheValue(BookDocumentPublicationModel(listOf(cachedDocument())), emptyMap())
            val large = small.copy(model = BookDocumentPublicationModel(listOf(cachedDocument("a".repeat(4096)))))

            assertTrue(cache.write(key, small))
            assertFalse(cache.write(key.copy(revision = "oversized"), large))
            assertEquals(small, cache.read(key))
            assertNull(cache.read(key.copy(revision = "oversized")))
            assertEquals(listOf("${key.diskKey()}.json"), directory.listFiles()!!.map { it.name })
        } finally {
            directory.deleteRecursively()
        }
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
