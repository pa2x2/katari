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
    fun `bubbles stacked beside a tall bubble are read as a column before it`() {
        val tall = "tall" to ImageRect(100, 50, 300, 900)
        val upperRight = "upper-right" to ImageRect(500, 40, 800, 300)
        val lowerRight = "lower-right" to ImageRect(500, 500, 800, 800)

        inReadingOrder(listOf(tall, lowerRight, upperRight), { it.second }, rightToLeft = true)
            .map { it.first } shouldContainExactly listOf("upper-right", "lower-right", "tall")
    }
}
