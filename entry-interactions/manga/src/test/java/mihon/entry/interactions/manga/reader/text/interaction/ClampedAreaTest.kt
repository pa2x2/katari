package mihon.entry.interactions.manga.reader.text.interaction

import io.kotest.matchers.shouldBe
import mihon.text.recognition.api.image.ImageRect
import org.junit.jupiter.api.Test

class ClampedAreaTest {

    @Test
    fun `an outline dragged in any direction past the page edge is clipped to the page`() {
        clampedArea(first = 900 to 1900, second = -40 to 1500, width = 1200, height = 1800) shouldBe
            ImageRect(0, 1500, 900, 1800)
    }

    @Test
    fun `an outline entirely beside the page selects nothing`() {
        clampedArea(first = 1300 to 100, second = 1500 to 400, width = 1200, height = 1800) shouldBe null
    }
}
