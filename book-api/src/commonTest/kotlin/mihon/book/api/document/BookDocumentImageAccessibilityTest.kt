package mihon.book.api.document

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BookDocumentImageAccessibilityTest {

    @Test
    fun `decorative image remains silent after serialization`() {
        val image = BookDocumentImage.withAccessibility(
            resourceId = "ornament",
            alternativeText = null,
            width = 16,
            height = 16,
            decorative = true,
        )

        val restored = Json.decodeFromString<BookDocumentImage>(Json.encodeToString(image))

        assertEquals(image, restored)
        assertEquals(true, restored.decorative)
        assertNull(restored.alternativeText)
    }
}
