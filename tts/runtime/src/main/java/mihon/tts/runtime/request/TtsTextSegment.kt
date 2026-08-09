package mihon.tts.runtime.request

internal data class TtsTextSegment(
    val text: String,
    val startOffset: Int,
)

internal fun segmentTtsText(
    text: String,
    maximumCodePoints: Int,
): List<TtsTextSegment> {
    require(maximumCodePoints > 0)
    if (text.codePointCount(0, text.length) <= maximumCodePoints) {
        return listOf(TtsTextSegment(text, 0))
    }

    return buildList {
        var start = 0
        while (start < text.length) {
            val remainingCodePoints = text.codePointCount(start, text.length)
            val hardEnd = if (remainingCodePoints <= maximumCodePoints) {
                text.length
            } else {
                text.offsetByCodePoints(start, maximumCodePoints)
            }
            val end = preferredBoundary(text, start, hardEnd)
            val segment = text.substring(start, end)
            if (segment.isNotBlank()) {
                add(TtsTextSegment(segment, start))
            }
            start = end
        }
    }
}

private fun preferredBoundary(text: String, start: Int, hardEnd: Int): Int {
    if (hardEnd == text.length) return hardEnd
    val minimumPreferredLength = (hardEnd - start) * 3 / 4
    var candidate = hardEnd
    while (candidate > start + minimumPreferredLength) {
        val previous = text[candidate - 1]
        if (previous.isWhitespace() || previous in SENTENCE_BOUNDARIES) {
            return candidate
        }
        candidate -= 1
    }
    return hardEnd
}

private val SENTENCE_BOUNDARIES = setOf('.', '!', '?', ';', ':', '。', '！', '？')
