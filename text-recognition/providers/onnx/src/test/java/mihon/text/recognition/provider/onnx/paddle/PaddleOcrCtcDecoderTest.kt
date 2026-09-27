package mihon.text.recognition.provider.onnx.paddle

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PaddleOcrCtcDecoderTest {

    private val dictionary = listOf("H", "I", "!")

    /** Scores where each step's best class is the given one; classes are blank, H, I, !, space. */
    private fun scores(vararg best: Int): FloatArray {
        val classes = dictionary.size + 2
        return FloatArray(best.size * classes) { index -> if (index % classes == best[index / classes]) 1f else 0f }
    }

    @Test
    fun `repeated classes collapse unless a blank separates them and the last class is a space`() {
        decodeCtc(scores(1, 1, 0, 2, 4, 1, 0, 1, 3), steps = 9, classes = 5, dictionary = dictionary) shouldBe
            "HI HH!"
    }
}
