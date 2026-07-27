package mihon.entry.interactions.book.document.resource

import org.junit.Test
import kotlin.test.assertEquals

class BookDocumentResourceValidationTest {

    @Test
    fun `image sampling respects rendered bounds and decoded pixel budget`() {
        assertEquals(
            8,
            proseImageSampleSize(
                sourceWidth = 8_000,
                sourceHeight = 8_000,
                targetWidth = 1_000,
                targetHeight = 1_000,
            ),
        )
        assertEquals(
            4,
            proseImageSampleSize(
                sourceWidth = 4_000,
                sourceHeight = 1_000,
                targetWidth = 1_000,
                targetHeight = 1_000,
            ),
        )
        assertEquals(
            1,
            proseImageSampleSize(
                sourceWidth = 800,
                sourceHeight = 600,
                targetWidth = 1_000,
                targetHeight = 1_000,
            ),
        )
    }
}
