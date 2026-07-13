package mihon.entry.interactions.book

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import mihon.book.api.BookLocator
import mihon.book.api.BookTextContext
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.EntryProgressLocator
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class BookProgressLocatorCodecTest {
    @Test
    fun `precise book locator round trips without duplicating common columns`() {
        val locator = BookLocator(
            resourceId = "OEBPS/chapter-2.xhtml",
            progression = 0.4,
            totalProgression = 0.7,
            logicalPosition = 12,
            fragments = listOf("paragraph-3"),
            textContext = BookTextContext(before = "before", highlight = "text", after = "after"),
            extensions = mapOf(
                "org.readium.locations" to JsonObject(mapOf("cssSelector" to JsonPrimitive("p:nth-child(3)"))),
            ),
        )

        val encoded = BookProgressLocatorCodec.encode(
            locator,
            preservedExtensions = JsonObject(mapOf("future.extension" to JsonPrimitive(true))),
        )

        assertEquals(BOOK_PROGRESS_LOCATOR_KIND, encoded.kind)
        assertEquals(12L, encoded.position)
        assertEquals(0.4, encoded.progression)
        assertEquals(0.7, encoded.totalProgression)
        assertEquals(locator, BookProgressLocatorCodec.decode(encoded))
        assertEquals(JsonPrimitive(true), encoded.extensions["future.extension"])
        val precise = encoded.extensions["app.katari.book.location"] as JsonObject
        assertFalse("progression" in precise)
        assertFalse("totalProgression" in precise)
        assertFalse("logicalPosition" in precise)
    }

    @Test
    fun `unsupported or imprecise progress locator cannot be restored`() {
        assertNull(BookProgressLocatorCodec.decode(EntryProgressLocator(kind = "page", position = 1)))
        assertNull(BookProgressLocatorCodec.decode(EntryProgressLocator(kind = BOOK_PROGRESS_LOCATOR_KIND)))
    }
}
