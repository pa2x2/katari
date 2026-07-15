package eu.kanade.tachiyomi.source.entry

import kotlinx.serialization.json.Json
import mihon.book.api.BookCatalogCoverage
import mihon.book.api.BookContentDescriptor
import mihon.book.api.BookResourceAvailability
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BookMediaTest {

    private val json = Json {
        classDiscriminator = "locationType"
    }

    @Test
    fun `book media keeps stable resource ids separate from retrieval locations`() {
        val media = EntryMedia.Book(
            descriptor = BookContentDescriptor(format = "application/epub+zip"),
            publicationRevision = "publication-v1",
            catalog = BookResourceCatalog(
                resources = listOf(
                    BookSourceResource(
                        id = "epub",
                        title = "EPUB",
                        order = 0,
                        availability = BookResourceAvailability.AVAILABLE,
                        location = BookResourceLocation.SourceChild(
                            resourceId = "epub",
                            sourceChildKey = "/download/epub",
                        ),
                    ),
                ),
                revision = "catalog-v2",
                coverage = BookCatalogCoverage.COMPLETE,
            ),
            initialResourceId = "epub",
            initialResourceLocation = BookResourceLocation.RemoteRequest(
                url = "https://example.invalid/book.epub",
                headers = mapOf("Referer" to "https://example.invalid/"),
            ),
        )

        assertEquals("epub", media.catalog.resources.single().id)
        assertEquals("publication-v1", media.publicationRevision)
        assertEquals(
            "/download/epub",
            (media.catalog.resources.single().location as BookResourceLocation.SourceChild).sourceChildKey,
        )
    }

    @Test
    fun `closed resource locations serialize without executable source behavior`() {
        val locations = listOf<BookResourceLocation>(
            BookResourceLocation.SourceChild("chapter-1", "/chapter/1"),
            BookResourceLocation.RemoteRequest("https://example.invalid/chapter", mapOf("Accept" to "text/html")),
            BookResourceLocation.InlineText("Chapter text", "text/plain"),
            BookResourceLocation.InlineBytes(byteArrayOf(1, 2, 3), "application/octet-stream"),
            BookResourceLocation.LocalUri("content://app.katari/book/1"),
            BookResourceLocation.AppReference("download:42"),
        )

        locations.forEach { location ->
            val restored = json.decodeFromString<BookResourceLocation>(json.encodeToString(location))
            assertEquals(location, restored)
        }
        assertContentEquals(
            byteArrayOf(1, 2, 3),
            (locations[3] as BookResourceLocation.InlineBytes).bytes,
        )
    }

    @Test
    fun `source boundary rejects unbounded inline content and duplicate catalog ids`() {
        assertFailsWith<IllegalArgumentException> {
            BookResourceLocation.InlineText(
                "x".repeat(BookResourceLocation.MAX_INLINE_TEXT_LENGTH + 1),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            BookResourceCatalog(
                resources = listOf(
                    BookSourceResource(id = "same"),
                    BookSourceResource(id = "same"),
                ),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            BookResourceLocation.RemoteRequest("file:///data/user/0/app.katari/book.epub")
        }
        assertFailsWith<IllegalArgumentException> {
            BookResourceLocation.LocalUri("/storage/emulated/0/book.epub")
        }
    }
}
