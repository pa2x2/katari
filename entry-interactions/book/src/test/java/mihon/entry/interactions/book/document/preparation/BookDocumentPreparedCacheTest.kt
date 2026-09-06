package mihon.entry.interactions.book.document.preparation

import mihon.book.api.document.BookDocumentPublicationModel
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
            model = BookDocumentPublicationModel(listOf(cachedDocument())),
            documentTitles = mapOf("chapter" to "Chapter"),
        )

        cache.write(key, value)

        assertEquals(value, cache.read(key))
        assertNull(cache.read(key.copy(revision = "different-digest")))
        assertNull(cache.read(key.copy(modelVersion = key.modelVersion + 1)))
        directory.deleteRecursively()
    }
}
