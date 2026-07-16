package mihon.entry.interactions.book.download

import android.app.Application
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.util.lang.Hash
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.book.api.BookCatalogCoverage
import mihon.book.api.BookContentDescriptor
import mihon.book.api.BookResourceCacheState
import mihon.book.api.BookResourceCapability
import mihon.entry.interactions.book.BookByteRange
import mihon.entry.interactions.book.BookMaterializationCache
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class DownloadedBookContentSessionTest {
    @Test
    fun `verified package exposes processor metadata streams and durable materialization`() = runTest {
        val packageFixture = downloadedPackage("downloaded chapter")
        val materializationDirectory = Files.createTempDirectory("katari-book-download-session").toFile()
        val materializationCache = BookMaterializationCache(
            application = mockk<Application>(relaxed = true),
            directory = materializationDirectory,
        )
        val session = DownloadedBookContentSession(packageFixture, materializationCache)

        val page = session.listResources(limit = 10).getOrThrow()
        val metadata = page.resources.single()
        assertEquals("publication-v1", session.revision)
        assertEquals(BookCatalogCoverage.PARTIAL, session.catalogCoverage)
        assertEquals(listOf("chapter"), session.primaryResourceIds)
        assertEquals(BookResourceCacheState.CACHED, metadata.cacheState)
        assertEquals(
            setOf(
                BookResourceCapability.STREAM,
                BookResourceCapability.RANGE,
                BookResourceCapability.MATERIALIZE,
            ),
            metadata.capabilities,
        )

        session.openResource("chapter", BookByteRange(11, 18)).getOrThrow().use { opened ->
            assertEquals("chapter", opened.stream.reader().readText())
        }
        val materialized = session.materializeResource("chapter").getOrThrow()
        assertEquals("downloaded chapter", materialized.file.readText())
        materialized.close()
        assertTrue(materialized.file.exists())

        session.close()
        assertTrue(session.getResource("chapter").isFailure)
        assertEquals(1, materializationCache.clear())
        assertFalse(materialized.file.exists())
    }

    @Test
    fun `session close releases outstanding downloaded streams`() = runTest {
        val packageFixture = downloadedPackage("chapter")
        val cache = BookMaterializationCache(
            application = mockk<Application>(relaxed = true),
            directory = Files.createTempDirectory("katari-book-download-close").toFile(),
        )
        val session = DownloadedBookContentSession(packageFixture, cache)
        val opened = session.openResource("chapter").getOrThrow()

        session.close()

        assertFailsWith<java.io.IOException> { opened.stream.read() }
    }
}

private fun downloadedPackage(content: String): VerifiedBookDownloadPackage {
    val root = Files.createTempDirectory("katari-downloaded-book-session").toFile()
    val provider = BookDownloadProvider(downloadsDirectory = { UniFile.fromFile(root) })
    val entry = Entry.create().copy(
        id = 1L,
        source = 42L,
        url = "/book/fixture",
        title = "Fixture Book",
        type = EntryType.BOOK,
    )
    val child = EntryChapter.create().copy(
        id = 11L,
        entryId = entry.id,
        url = "/chapter/1",
        name = "Chapter 1",
    )
    val staging = provider.beginPackage("Fixture Source", entry, child).getOrThrow()
    val bytes = content.encodeToByteArray()
    val fileName = provider.resourceFileName("chapter", "text/html")
    staging.directory.createFile(fileName)!!.openOutputStream().use { it.write(bytes) }
    val manifest = BookDownloadManifest(
        sourceId = entry.source,
        entryId = entry.id,
        entryTitle = entry.title,
        entryUrl = entry.url,
        childId = child.id,
        childTitle = child.name,
        childUrl = child.url,
        descriptor = BookContentDescriptor("text/html", profile = "prose-chapter"),
        publicationId = "source:42:entry:/book/fixture",
        publicationRevision = "publication-v1",
        catalogRevision = "catalog-v1",
        catalogCoverage = BookCatalogCoverage.PARTIAL,
        primaryResourceIds = listOf("chapter"),
        resources = listOf(
            BookDownloadedResource(
                id = "chapter",
                title = child.name,
                mediaType = "text/html",
                revision = "chapter-v1",
                fileName = fileName,
                storedSize = bytes.size.toLong(),
                sha256 = Hash.sha256(bytes),
            ),
        ),
        createdAt = 1L,
    )
    return provider.completePackage(staging, manifest).getOrThrow()
}
