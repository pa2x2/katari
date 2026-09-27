package mihon.text.recognition.provider.onnx.tensor

import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * Expected values are Pillow's `Image.resize(..., BILINEAR)` output, the preprocessing the models were trained with.
 * Pillow's fixed-point weights may round a level differently.
 */
class PixelResamplingTest {

    private fun gray(vararg values: Int) = IntArray(values.size) { index ->
        val value = values[index]
        (0xFF shl 24) or (value shl 16) or (value shl 8) or value
    }

    private infix fun IntArray.shouldMatch(expected: List<Int>) {
        map { it.blue() }.zip(expected).forEach { (actual, level) -> actual shouldBeInRange level - 1..level + 1 }
        size shouldBe expected.size
    }

    @Test
    fun `a reduction averages every source pixel so a thin stroke still darkens the result`() {
        val row = intArrayOf(255, 255, 255, 0, 255, 255, 128, 64)
        val pixels = gray(*row, *row.reversedArray())

        resamplePixels(pixels, 8, 2, 2, 2) shouldMatch listOf(209, 162, 162, 209)
        resamplePixels(pixels, 8, 2, 3, 1) shouldMatch listOf(193, 173, 193)
    }

    @Test
    fun `an enlargement interpolates between neighbouring pixels`() {
        resamplePixels(gray(0, 255, 255, 0), 2, 2, 4, 4) shouldMatch listOf(
            0, 64, 191, 255,
            64, 96, 159, 191,
            191, 159, 96, 64,
            255, 191, 64, 0,
        )
    }
}
