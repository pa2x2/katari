package mihon.text.recognition.runtime.geometry

import io.kotest.matchers.collections.shouldContainExactly
import mihon.text.recognition.api.image.ImageRect
import org.junit.jupiter.api.Test

class ReadingOrderTest {

    // Two bubbles side by side in the top strip (tops not aligned), one bubble below.
    private val topRight = "top-right" to ImageRect(700, 40, 900, 300)
    private val topLeft = "top-left" to ImageRect(100, 90, 300, 260)
    private val bottom = "bottom" to ImageRect(300, 700, 500, 900)

    @Test
    fun `right-to-left pages are read from the right within each row`() {
        inReadingOrder(listOf(bottom, topLeft, topRight), { it.second }, rightToLeft = true)
            .map { it.first } shouldContainExactly listOf("top-right", "top-left", "bottom")
    }

    @Test
    fun `left-to-right pages are read from the left within each row`() {
        inReadingOrder(listOf(bottom, topRight, topLeft), { it.second }, rightToLeft = false)
            .map { it.first } shouldContainExactly listOf("top-left", "top-right", "bottom")
    }
}
