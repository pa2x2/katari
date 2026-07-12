package mihon.book.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals

class BookModelsTest {

    @Test
    fun `locator serialization is independent of a processor engine`() {
        val locator = BookLocator(
            resourceId = "text/chapter-1.xhtml",
            progression = 0.25,
            totalProgression = 0.1,
            logicalPosition = 4,
            fragments = listOf("chapter-start"),
            textContext = BookTextContext(highlight = "A stable excerpt"),
            extensions = mapOf(
                "example.reader.precision" to buildJsonObject {
                    put("selector", "p:nth-child(4)")
                },
            ),
        )

        val restored = Json.decodeFromString<BookLocator>(Json.encodeToString(locator))

        assertEquals(locator, restored)
    }

    @Test
    fun `locator rejects invalid normalized positions`() {
        listOf(-0.1, 1.1, Double.NaN, Double.POSITIVE_INFINITY).forEach { invalid ->
            kotlin.test.assertFailsWith<IllegalArgumentException> {
                BookLocator(resourceId = "chapter", progression = invalid)
            }
            kotlin.test.assertFailsWith<IllegalArgumentException> {
                BookLocator(resourceId = "chapter", totalProgression = invalid)
            }
        }
        kotlin.test.assertFailsWith<IllegalArgumentException> {
            BookLocator(resourceId = "chapter", logicalPosition = 0)
        }
    }

    @Test
    fun `text context is bounded at the stable model boundary`() {
        kotlin.test.assertFailsWith<IllegalArgumentException> {
            BookTextContext(highlight = "x".repeat(BookTextContext.MAX_LENGTH + 1))
        }
    }
}
