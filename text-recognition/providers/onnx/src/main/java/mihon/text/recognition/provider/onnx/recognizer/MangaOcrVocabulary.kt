package mihon.text.recognition.provider.onnx.recognizer

import java.text.Normalizer

/**
 * Character-level BERT vocabulary of Manga OCR. Decoding follows the upstream post-processing: special tokens and
 * whitespace are dropped, ellipses are spelled with full stops, runs of middle dots become full stops, and half-width
 * characters are widened as `jaconv.h2z` does, since the model spells Japanese text with full-width ones.
 */
internal class MangaOcrVocabulary(
    private val tokens: List<String>,
) {
    fun decode(ids: IntArray): String {
        val text = ids
            .filter { it > LAST_SPECIAL_TOKEN && it < tokens.size }
            .joinToString("") { tokens[it].removePrefix(CONTINUATION_PREFIX) }
            .filterNot(Char::isWhitespace)
            .replace("…", "...")
        return toFullWidth(DOT_RUN.replace(text) { match -> ".".repeat(match.value.length) })
    }

    companion object {
        const val START_TOKEN = 2
        const val END_TOKEN = 3

        /** `[PAD]`, `[UNK]`, `[CLS]`, `[SEP]`, and `[MASK]` occupy the first ids. */
        private const val LAST_SPECIAL_TOKEN = 4
        private const val CONTINUATION_PREFIX = "##"
        private val DOT_RUN = Regex("[・.]{2,}")
        private val HALF_WIDTH_KANA = Regex("[｡-ﾟ]+")
        private const val FULL_WIDTH_OFFSET = 0xFEE0

        fun parse(lines: Sequence<String>): MangaOcrVocabulary = MangaOcrVocabulary(lines.toList())

        /** ASCII letters, digits, and symbols become their full-width forms; half-width kana become full-width kana. */
        private fun toFullWidth(text: String): String {
            val widened = buildString(text.length) {
                text.forEach { char -> append(if (char in '!'..'~') char + FULL_WIDTH_OFFSET else char) }
            }
            // NFKC composes a half-width kana with its sound mark, as jaconv does.
            return HALF_WIDTH_KANA.replace(widened) { Normalizer.normalize(it.value, Normalizer.Form.NFKC) }
        }
    }
}
