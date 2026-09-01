package mihon.entry.interactions.book.document.resource

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
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

    @Test
    fun `static SVG is decoded through the generic document image path`() {
        val bitmap = decodeValidatedProseImage(
            bytes = """
                <svg xmlns="http://www.w3.org/2000/svg" width="40" height="20">
                  <script>throw new Error('must not execute')</script>
                  <rect width="40" height="20" fill="#336699"/>
                </svg>
            """.trimIndent().encodeToByteArray(),
            mediaType = "image/svg+xml",
            targetWidthPx = 80,
            targetHeightPx = 80,
        )

        assertEquals(40, bitmap.width)
        assertEquals(20, bitmap.height)
        bitmap.recycle()
    }
}
