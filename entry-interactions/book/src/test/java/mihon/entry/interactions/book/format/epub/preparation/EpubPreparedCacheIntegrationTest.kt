package mihon.entry.interactions.book.format.epub.preparation

import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.book.document.preparation.BookDocumentPreparedCache
import mihon.entry.interactions.book.document.preparation.PreparedBookDocumentPublication
import mihon.entry.interactions.book.preparation.BookPreparationResult
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class EpubPreparedCacheIntegrationTest {
    @Test
    fun `book remains readable when the cache entry exceeds its budget`() = runTest {
        val directory = Files.createTempDirectory("epub-cache-budget").toFile()
        try {
            assertReadable(BookDocumentPreparedCache(RuntimeEnvironment.getApplication(), directory, maxEntryBytes = 1))
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun `book remains readable when cache storage cannot be read or written`() = runTest {
        val unavailableDirectory = Files.createTempFile("epub-cache-unavailable", ".file").toFile()
        try {
            assertReadable(BookDocumentPreparedCache(RuntimeEnvironment.getApplication(), unavailableDirectory))
        } finally {
            unavailableDirectory.delete()
        }
    }

    private suspend fun assertReadable(cache: BookDocumentPreparedCache) {
        val file = epubPublicationFile()
        try {
            val result = EpubBookPreparer(preparedCache = cache).prepare(EpubContentSessionFixture(file))
            assertIs<BookPreparationResult.Success>(result).publication.use { prepared ->
                val publication = assertIs<PreparedBookDocumentPublication>(prepared)
                assertEquals(listOf("OPS/one.xhtml", "OPS/two.xhtml"), publication.documents.map { it.resourceId })
                assertTrue(publication.documents.first().blocks.any { it.plainText == "Continue" })
            }
        } finally {
            file.delete()
        }
    }
}
