package app.katari.entry.interactions.book.format.epub.preparation

import app.katari.entry.interactions.book.document.preparation.BookDocumentPreparedCache
import app.katari.entry.interactions.book.document.preparation.PreparedBookDocumentPublication
import app.katari.entry.interactions.book.preparation.BookPreparationResult
import kotlinx.coroutines.test.runTest
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
    fun `book remains readable when the cache entry exceeds its budget or storage is unavailable`() = runTest {
        val directory = Files.createTempDirectory("epub-cache-budget").toFile()
        val unavailableDirectory = Files.createTempFile("epub-cache-unavailable", ".file").toFile()
        try {
            assertReadable(BookDocumentPreparedCache(directory, maxEntryBytes = 1))
            assertReadable(BookDocumentPreparedCache(unavailableDirectory))
        } finally {
            directory.deleteRecursively()
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
