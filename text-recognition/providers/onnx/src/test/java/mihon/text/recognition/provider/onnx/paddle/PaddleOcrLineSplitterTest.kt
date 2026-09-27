package mihon.text.recognition.provider.onnx.paddle

import io.kotest.matchers.collections.shouldContainExactly
import mihon.text.recognition.api.image.ImageRect
import org.junit.jupiter.api.Test

class PaddleOcrLineSplitterTest {

    /** A probability map with text in the given rectangles. */
    private fun map(width: Int, height: Int, vararg text: ImageRect): FloatArray =
        FloatArray(width * height) { index ->
            val x = index % width
            val y = index / width
            if (text.any { it.contains(x, y) }) 0.9f else 0.05f
        }

    @Test
    fun `horizontal lines are split into padded bands from top to bottom`() {
        val lines = splitLines(
            probability = map(100, 60, ImageRect(20, 10, 80, 18), ImageRect(30, 30, 70, 38)),
            width = 100,
            height = 60,
            vertical = false,
        )

        lines shouldContainExactly listOf(ImageRect(16, 6, 84, 22), ImageRect(26, 26, 74, 42))
    }

    @Test
    fun `vertical lines are split into columns read from right to left`() {
        val lines = splitLines(
            probability = map(60, 100, ImageRect(10, 20, 18, 80), ImageRect(30, 10, 38, 90)),
            width = 60,
            height = 100,
            vertical = true,
        )

        lines shouldContainExactly listOf(ImageRect(26, 6, 42, 94), ImageRect(6, 16, 22, 84))
    }

    @Test
    fun `specks thinner than a line are ignored`() {
        splitLines(map(100, 60, ImageRect(40, 30, 60, 31)), 100, 60, vertical = false) shouldContainExactly emptyList()
    }
}
