package mihon.text.recognition.provider.onnx.recognizer

/**
 * Character-level BERT vocabulary of Manga OCR. Decoding follows the upstream post-processing: special tokens and
 * whitespace are dropped, ellipses are spelled with full stops, and runs of middle dots become full stops.
 */
internal class MangaOcrVocabulary(
    private val tokens: List<String>,
) {
    fun decode(ids: List<Int>): String {
        val text = ids
            .filter { it > LAST_SPECIAL_TOKEN && it < tokens.size }
            .joinToString("") { tokens[it].removePrefix(CONTINUATION_PREFIX) }
            .filterNot(Char::isWhitespace)
            .replace("…", "...")
        return DOT_RUN.replace(text) { match -> ".".repeat(match.value.length) }
    }

    companion object {
        const val START_TOKEN = 2
        const val END_TOKEN = 3

        /** `[PAD]`, `[UNK]`, `[CLS]`, `[SEP]`, and `[MASK]` occupy the first ids. */
        private const val LAST_SPECIAL_TOKEN = 4
        private const val CONTINUATION_PREFIX = "##"
        private val DOT_RUN = Regex("[・.]{2,}")

        fun parse(lines: Sequence<String>): MangaOcrVocabulary = MangaOcrVocabulary(lines.toList())
    }
}
