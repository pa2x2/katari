package mihon.text.recognition.provider.onnx.recognizer

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow

/**
 * Beam search with the generation settings Manga OCR publishes in its model configuration: four beams, no repeated
 * trigram, a length penalty of 2, and stopping as soon as every beam has finished.
 *
 * Greedy decoding reads ambiguous characters worse and, without the trigram rule, can repeat a phrase until the
 * length limit, which costs hundreds of decoder runs for one bubble.
 */
internal class MangaOcrBeamSearch(
    private val startToken: Int,
    private val endToken: Int,
    private val beams: Int = BEAMS,
    private val noRepeatNgram: Int = NO_REPEAT_NGRAM,
    private val lengthPenalty: Double = LENGTH_PENALTY,
    private val maximumLength: Int = MAXIMUM_LENGTH,
) {
    /**
     * Decodes one sequence. [nextLogits] receives the running sequences (each starting with the start token) and
     * returns, for each of them, the logits of the token that follows it.
     *
     * @return the best sequence without its start and end tokens.
     */
    suspend fun search(nextLogits: suspend (sequences: List<IntArray>) -> List<FloatArray>): IntArray {
        var running = listOf(Beam(intArrayOf(startToken), 0.0))
        val finished = mutableListOf<Finished>()
        while (true) {
            val logits = nextLogits(running.map(Beam::tokens))
            val candidates = TopCandidates(2 * beams)
            running.forEachIndexed { index, beam ->
                val scores = logSoftmax(logits[index])
                bannedTokens(beam.tokens).forEach { scores[it] = Double.NEGATIVE_INFINITY }
                scores.forEachIndexed { token, score -> candidates.offer(index, token, beam.score + score) }
            }
            val next = mutableListOf<Beam>()
            candidates.sorted().forEachIndexed { rank, candidate ->
                if (next.size == beams) return@forEachIndexed
                val parent = running[candidate.beam]
                if (candidate.token == endToken) {
                    // Only a top-ranked end is a real finish; lower ones would have lost to continuing beams.
                    if (rank < beams) finished += Finished(parent.tokens, normalized(candidate.score, parent.tokens))
                } else {
                    next += Beam(parent.tokens + candidate.token, candidate.score)
                }
            }
            if (finished.size >= beams || next.isEmpty() || next.first().tokens.size >= maximumLength) {
                if (finished.size < beams) {
                    next.forEach { finished += Finished(it.tokens, normalized(it.score, it.tokens)) }
                }
                break
            }
            running = next
        }
        val best = finished.maxBy(Finished::score).tokens
        return best.copyOfRange(1, best.size)
    }

    private fun normalized(score: Double, tokens: IntArray) = score / tokens.size.toDouble().pow(lengthPenalty)

    /** Tokens that would complete an n-gram the sequence already contains. */
    private fun bannedTokens(tokens: IntArray): Set<Int> {
        if (tokens.size + 1 < noRepeatNgram) return emptySet()
        val prefixStart = tokens.size - (noRepeatNgram - 1)
        return (0..tokens.size - noRepeatNgram).mapNotNullTo(HashSet()) { start ->
            val matches = (0 until noRepeatNgram - 1).all { tokens[start + it] == tokens[prefixStart + it] }
            if (matches) tokens[start + noRepeatNgram - 1] else null
        }
    }

    private fun logSoftmax(logits: FloatArray): DoubleArray {
        val maximum = logits.max().toDouble()
        val sum = logits.sumOf { exp(it - maximum) }
        val offset = maximum + ln(sum)
        return DoubleArray(logits.size) { logits[it] - offset }
    }

    private class Beam(val tokens: IntArray, val score: Double)

    private class Finished(val tokens: IntArray, val score: Double)

    private data class Candidate(val beam: Int, val token: Int, val score: Double)

    /** The [capacity] highest-scoring candidates offered, without sorting every token of every beam. */
    private class TopCandidates(private val capacity: Int) {
        private val kept = ArrayList<Candidate>(capacity + 1)

        fun offer(beam: Int, token: Int, score: Double) {
            if (score == Double.NEGATIVE_INFINITY) return
            if (kept.size == capacity && score <= kept.last().score) return
            val index = kept.indexOfFirst { score > it.score }.let { if (it < 0) kept.size else it }
            kept.add(index, Candidate(beam, token, score))
            if (kept.size > capacity) kept.removeAt(kept.lastIndex)
        }

        fun sorted(): List<Candidate> = kept
    }

    private companion object {
        const val BEAMS = 4
        const val NO_REPEAT_NGRAM = 3
        const val LENGTH_PENALTY = 2.0
        const val MAXIMUM_LENGTH = 300
    }
}
