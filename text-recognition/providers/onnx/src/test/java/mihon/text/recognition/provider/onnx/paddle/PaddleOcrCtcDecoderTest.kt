package mihon.text.recognition.provider.onnx.paddle

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PaddleOcrCtcDecoderTest {

    private val dictionary = listOf("H", "I", "!")

    /** Probabilities where each step's best class is the given one; classes are blank, H, I, !, space. */
    private fun scores(vararg best: Pair<Int, Float>): FloatArray {
        val classes = dictionary.size + 2
        return FloatArray(best.size * classes) { index ->
            val (bestClass, probability) = best[index / classes]
            if (index % classes == bestClass) probability else (1f - probability) / (classes - 1)
        }
    }

    @Test
    fun `repeated classes collapse unless a blank separates them and the last class is a space`() {
        val steps = listOf(1, 1, 0, 2, 4, 1, 0, 1, 3).map { it to 1f }.toTypedArray()

        decodeCtc(scores(*steps), steps = 9, classes = 5, dictionary = dictionary).text shouldBe "HI HH!"
    }
}
