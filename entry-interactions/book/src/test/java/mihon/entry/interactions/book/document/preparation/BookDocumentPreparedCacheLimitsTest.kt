package mihon.entry.interactions.book.document.preparation

import mihon.book.api.document.BookDocumentPublicationModel
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
class BookDocumentPreparedCacheLimitsTest {
    @Test
    fun `default cache accepts books larger than the former 16 MiB entry limit`() {
        val directory = Files.createTempDirectory("book-document-large-cache").toFile()
        try {
            val cache = BookDocumentPreparedCache(RuntimeEnvironment.getApplication(), directory)
            val key = BookDocumentPreparedCacheKey("publication", "revision")
            val value = BookDocumentPreparedCacheValue(
                BookDocumentPublicationModel(
                    List(12) { index -> cachedDocument("a".repeat(600 * 1024), resourceId = "chapter$index") },
                ),
                emptyMap(),
            )

            assertTrue(cache.write(key, value))
            assertTrue(directory.resolve("${key.diskKey()}.json").length() > 16L * 1024 * 1024)
            assertEquals(value, cache.read(key))
        } finally {
            directory.deleteRecursively()
        }
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
}
