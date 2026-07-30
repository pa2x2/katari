package mihon.entry.interactions.book.epub

import android.app.Application
import com.hippo.unifile.UniFile
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import mihon.book.api.BookCatalogCoverage
import mihon.book.api.BookContentDescriptor
import mihon.book.api.BookFailureReason
import mihon.book.api.BookLocator
import mihon.book.api.BookReadingDirection
import mihon.book.api.BookResourceCapability
import mihon.book.api.BookTextContext
import mihon.entry.interactions.book.BookMaterializationCache
import mihon.entry.interactions.book.BookOpenResult
import mihon.entry.interactions.book.download.BookDownloadManifest
import mihon.entry.interactions.book.download.BookDownloadedResource
import mihon.entry.interactions.book.download.DownloadedBookContentSession
import mihon.entry.interactions.book.download.VerifiedBookDownloadPackage
import mihon.entry.viewer.settings.StandardReaderCapabilities
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ReadiumEpubProcessorTest {

    @Test
    fun `supports only unprotected reflowable EPUB descriptors`() {
        val processor = ReadiumEpubProcessor()

        assertTrue(processor.supports(BookContentDescriptor("application/epub+zip")))
        assertTrue(processor.supports(BookContentDescriptor("application/epub+zip", profile = "reflowable")))
        assertFalse(processor.supports(BookContentDescriptor("application/epub+zip", profile = "fixed-layout")))
        assertFalse(processor.supports(BookContentDescriptor("application/epub+zip", protection = "drm")))
        assertFalse(processor.supports(BookContentDescriptor("text/html")))
    }

    @Test
    fun `does not advertise adjacent chapter preparation`() {
        assertFalse(
            StandardReaderCapabilities.NextChapterPreparation in
                ReadiumEpubProcessor().potentialReaderCapabilities,
        )
    }

    @Test
    fun `opens authored EPUB 2 and maps reading order and nested navigation`() = runBlocking {
        val fixture = EpubFixture.write(temporaryDirectory().resolve("epub2.epub"), version = 2)
        val content = TestContentSession(fixture, publicationId = "book:epub2", revision = "v1")

        val result = ReadiumEpubProcessor().open(content)

        val session = assertIs<BookOpenResult.Success>(result, result.toString()).session
        assertEquals("book:epub2", session.publication.id)
        assertEquals("v1", session.publication.revision)
        assertEquals(2, session.publication.readingOrder.size)
        assertEquals("Part One", session.publication.navigation.single().title)
        assertEquals("Chapter One", session.publication.navigation.single().children.single().title)
        assertFalse(content.closed)
        assertEquals(0, content.leaseCloseCount.get())

        session.close()
        session.close()
        assertEquals(1, content.leaseCloseCount.get())
    }

    @Test
    fun `opens an authored EPUB from a durable downloaded content session`() = runBlocking {
        val fixture = EpubFixture.write(temporaryDirectory().resolve("downloaded.epub"), version = 2)
        val downloadedFile = mockk<UniFile> {
            every { openInputStream() } answers { fixture.inputStream() }
        }
        val manifest = BookDownloadManifest(
            sourceId = 9L,
            entryId = 1L,
            entryTitle = "Downloaded EPUB",
            entryUrl = "/book",
            childId = 10L,
            childTitle = "EPUB",
            childUrl = "/book.epub",
            descriptor = BookContentDescriptor("application/epub+zip"),
            publicationId = "book:downloaded",
            publicationRevision = "v1",
            catalogCoverage = BookCatalogCoverage.COMPLETE,
            primaryResourceIds = listOf("publication.epub"),
            resources = listOf(
                BookDownloadedResource(
                    id = "publication.epub",
                    mediaType = "application/epub+zip",
                    revision = "v1",
                    fileName = "publication.epub",
                    storedSize = fixture.length(),
                    sha256 = "0".repeat(64),
                ),
            ),
            createdAt = 1L,
        )
        val downloaded = VerifiedBookDownloadPackage(
            directory = mockk(relaxed = true),
            manifest = manifest,
            resources = mapOf("publication.epub" to downloadedFile),
        )
        val materializationCache = BookMaterializationCache(
            application = mockk<Application>(relaxed = true),
            directory = temporaryDirectory().resolve("materialized").toFile(),
        )
        val content = DownloadedBookContentSession(downloaded, materializationCache)

        val result = assertIs<BookOpenResult.Success>(ReadiumEpubProcessor().open(content))

        assertEquals("book:downloaded", result.session.publication.id)
        assertEquals(2, result.session.publication.readingOrder.size)
        result.session.close()
        content.close()
    }

    @Test
    fun `opens authored EPUB 3 and preserves RTL and anchored navigation`() = runBlocking {
        val fixture = EpubFixture.write(temporaryDirectory().resolve("epub3.epub"), version = 3, rtl = true)
        val content = TestContentSession(fixture, publicationId = "book:epub3", revision = "v2")

        val result = ReadiumEpubProcessor().open(content)
        val session = assertIs<BookOpenResult.Success>(result, result.toString()).session as ReadiumPublicationSession
        val publication = session.publication

        assertEquals(BookReadingDirection.RIGHT_TO_LEFT, publication.readingDirection)
        assertEquals(listOf("ar"), publication.languages)
        val chapterTarget = publication.navigation.single().children.single().target
        assertTrue(chapterTarget.resourceId.endsWith("chapter1.xhtml"))
        assertEquals(listOf("intro"), chapterTarget.fragments)
        assertTrue(publication.readingOrder.any { it.mediaType == "application/xhtml+xml" })

        val locator = BookLocator(
            resourceId = publication.readingOrder.first().id,
            progression = 0.4,
            totalProgression = 0.2,
            logicalPosition = 3,
            fragments = listOf("intro"),
            textContext = BookTextContext(
                before = "before",
                highlight = "highlight",
                after = "after",
            ),
            extensions = mapOf("org.readium.locations" to JsonPrimitive("extension")),
        )
        val readiumLocator = checkNotNull(ReadiumLocatorAdapter.restore(locator, session.readiumPublication()))
        val restored = ReadiumLocatorAdapter.adapt(readiumLocator)
        assertEquals(locator.copy(extensions = emptyMap()), restored.copy(extensions = emptyMap()))
        assertTrue(session.validate(locator))

        val migrated = checkNotNull(
            session.reconcileMigratedLocator(
                BookLocator(resourceId = "missing-source-resource.xhtml", totalProgression = 0.2),
            ),
        )
        assertTrue(session.validate(migrated))

        session.close()
    }

    @Test
    fun `reports malformed content and releases materialized lease`() = runBlocking {
        val malformed = temporaryDirectory().resolve("malformed.epub").toFile().apply {
            writeText("not an epub")
        }
        val content = TestContentSession(malformed, publicationId = "book:bad", revision = "v1")

        val result = assertIs<BookOpenResult.Failure>(ReadiumEpubProcessor().open(content))

        assertEquals(BookFailureReason.MALFORMED_CONTENT, result.failure.reason)
        assertEquals(1, content.leaseCloseCount.get())
        assertEquals(1, content.leaseInvalidationCount.get())
        assertFalse(content.closed)
    }

    @Test
    fun `rejects fixed-layout EPUB discovered during parsing`() = runBlocking {
        val fixture = EpubFixture.write(
            temporaryDirectory().resolve("fixed-layout.epub"),
            version = 3,
            fixedLayout = true,
        )
        val content = TestContentSession(fixture, publicationId = "book:fixed", revision = "v1")

        val result = assertIs<BookOpenResult.Failure>(ReadiumEpubProcessor().open(content))

        assertEquals(BookFailureReason.FORMAT_UNSUPPORTED, result.failure.reason)
        assertEquals(1, content.leaseCloseCount.get())
        assertEquals(0, content.leaseInvalidationCount.get())
    }

    @Test
    fun `requires a materializable primary resource`() = runBlocking {
        val fixture = EpubFixture.write(temporaryDirectory().resolve("stream-only.epub"), version = 3)
        val content = TestContentSession(
            fixture,
            publicationId = "book:stream-only",
            revision = "v1",
            capabilities = setOf(BookResourceCapability.STREAM),
        )

        val result = assertIs<BookOpenResult.Failure>(ReadiumEpubProcessor().open(content))

        assertEquals(BookFailureReason.CONTENT_UNAVAILABLE, result.failure.reason)
        assertEquals(0, content.leaseCloseCount.get())
    }

    private fun temporaryDirectory(): Path = Files.createTempDirectory("katari-readium-spike")
}
