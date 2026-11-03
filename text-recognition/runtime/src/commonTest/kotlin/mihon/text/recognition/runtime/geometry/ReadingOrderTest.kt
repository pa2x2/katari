package mihon.text.recognition.runtime.geometry

import io.kotest.matchers.collections.shouldContainExactly
import mihon.text.recognition.api.image.ImageRect
import org.junit.jupiter.api.Test

class ReadingOrderTest {

    @Test
    fun `bubbles stacked beside a tall bubble are read as a column before it`() {
        val tall = "tall" to ImageRect(100, 50, 300, 900)
        val upperRight = "upper-right" to ImageRect(500, 40, 800, 300)
        val lowerRight = "lower-right" to ImageRect(500, 500, 800, 800)

        inReadingOrder(listOf(tall, lowerRight, upperRight), { it.second }, rightToLeft = true)
            .map { it.first } shouldContainExactly listOf("upper-right", "lower-right", "tall")
    }
}
