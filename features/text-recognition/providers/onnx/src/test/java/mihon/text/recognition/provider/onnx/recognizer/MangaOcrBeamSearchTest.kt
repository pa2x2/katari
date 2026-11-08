package mihon.text.recognition.provider.onnx.recognizer

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.math.ln

class MangaOcrBeamSearchTest {

    private val search = MangaOcrBeamSearch(startToken = START, endToken = END)

    /** Logits whose softmax gives [probabilities] (token to probability); every other token is nearly impossible. */
    private fun logits(vararg probabilities: Pair<Int, Double>) = FloatArray(VOCABULARY) { token ->
        ln(probabilities.firstOrNull { it.first == token }?.second ?: 1e-9).toFloat()
    }

    @Test
    fun `a sequence whose first token is less likely than another's is found when its continuation is certain`() =
        runTest {
            // Greedy decoding takes A (0.5) and then faces a five-way split; B (0.4) is followed by the end for certain.
            val result = search.search { sequences ->
                sequences.map { sequence ->
                    when (sequence.drop(1)) {
                        emptyList<Int>() -> logits(A to 0.5, B to 0.4, C to 0.1)
                        listOf(A) -> logits(C to 0.2, D to 0.2, E to 0.2, F to 0.2, END to 0.2)
                        else -> logits(END to 1.0)
                    }
                }
            }

            result.toList() shouldContainExactly listOf(B)
        }

    @Test
    fun `a model that keeps repeating a phrase cannot repeat any three tokens and finishes`() = runTest {
        var steps = 0
        val result = search.search { sequences ->
            steps++
            // Always prefers continuing the cycle A B A B ..., and ends only as a last resort.
            sequences.map { sequence ->
                val next = if (sequence.last() == A) B else A
                logits(next to 0.9, END to 0.05, C to 0.05)
            }
        }

        (listOf(START) + result.toList()).windowed(3).groupingBy { it }.eachCount().values.forEach {
            it shouldBe 1
        }
        steps shouldBeLessThan 10
    }

    private companion object {
        const val START = 0
        const val END = 1
        const val A = 2
        const val B = 3
        const val C = 4
        const val D = 5
        const val E = 6
        const val F = 7
        const val VOCABULARY = 8
    }
}
