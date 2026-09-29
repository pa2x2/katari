package mihon.text.recognition.provider.onnx.paddle.lines

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class DbTextLinesTest {

    /** A [width]×[height] probability map with [probability] wherever [text] holds and 0.05 elsewhere. */
    private fun map(width: Int, height: Int, probability: Float = 0.9f, text: (x: Int, y: Int) -> Boolean) =
        FloatArray(width * height) { index -> if (text(index % width, index / width)) probability else 0.05f }

    @Test
    fun `blocks side by side on the same rows are separate lines grown by PaddleOCR's unclip margin`() {
        val lines = detectTextLines(
            map(100, 60) { x, y -> y in 20 until 28 && (x in 10 until 40 || x in 60 until 90) },
            width = 100,
            height = 60,
        ).sortedBy { it.centerX }

        lines shouldHaveSize 2
        // A 30×8 area grows by area × 1.5 / perimeter on every side.
        val margin = 30.0 * 8 * 1.5 / (2 * (30 + 8))
        lines.forEach { line ->
            line.width shouldBe (30 + 2 * margin plusOrMinus 0.01)
            line.height shouldBe (8 + 2 * margin plusOrMinus 0.01)
            line.centerY shouldBe (24.0 plusOrMinus 0.01)
        }
        lines[0].centerX shouldBe (25.0 plusOrMinus 0.01)
        lines[1].centerX shouldBe (75.0 plusOrMinus 0.01)
    }
}
