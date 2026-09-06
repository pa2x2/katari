package eu.kanade.tachiyomi.source.entry

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BookMediaTest {

    private val json = Json {
        classDiscriminator = "locationType"
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
