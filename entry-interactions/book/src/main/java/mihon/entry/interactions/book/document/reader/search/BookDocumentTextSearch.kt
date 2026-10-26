package mihon.entry.interactions.book.document.reader.search

import java.text.Normalizer

/** A found range of a document's canonical text, from [start] up to [endExclusive]. */
internal data class BookDocumentTextMatch(val start: Int, val endExclusive: Int)

/**
 * The places [query] occurs in [text], ignoring case and accents and treating every kind of space alike, in order and
 * without overlaps. Ranges are offsets in [text] as it is, so a match against an accent written as a separate
 * combining mark still covers the mark.
 */
internal fun bookDocumentTextMatches(text: String, query: String): List<BookDocumentTextMatch> {
    val needle = FoldedText(query).folded
    if (needle.isBlank()) return emptyList()
    val haystack = FoldedText(text)
    return buildList {
        var from = 0
        while (true) {
            val index = haystack.folded.indexOf(needle, from)
            if (index < 0) break
            val start = haystack.sourceOffset(index)
            add(BookDocumentTextMatch(start, haystack.sourceOffset(index + needle.length).coerceAtLeast(start + 1)))
            from = index + needle.length
        }
    }
}

/**
 * The text of a match with some of what surrounds it, cut at word boundaries, on one line. [matchStart] and [matchEnd]
 * locate the match within [text].
 */
internal data class BookDocumentSearchExcerpt(val text: String, val matchStart: Int, val matchEnd: Int)

internal fun bookDocumentSearchExcerpt(text: String, match: BookDocumentTextMatch): BookDocumentSearchExcerpt {
    var start = (match.start - EXCERPT_BEFORE).coerceAtLeast(0)
    if (start > 0) {
        start = (start until match.start).firstOrNull { text[it].isWhitespace() }?.plus(1) ?: start
    }
    var end = (match.endExclusive + EXCERPT_AFTER).coerceAtMost(text.length)
    if (end < text.length) {
        end = (end downTo match.endExclusive).firstOrNull { text[it].isWhitespace() } ?: end
    }
    val prefix = if (start > 0) ELLIPSIS else ""
    val suffix = if (end < text.length) ELLIPSIS else ""
    val body = text.substring(start, end).map { if (it.isWhitespace()) ' ' else it }.joinToString("")
    val matchStart = prefix.length + match.start - start
    return BookDocumentSearchExcerpt(
        text = prefix + body + suffix,
        matchStart = matchStart,
        matchEnd = matchStart + match.endExclusive - match.start,
    )
}

/**
 * [source] lower-cased, without accents and with every space a plain one, remembering where each character came from.
 */
private class FoldedText(private val source: String) {
    // Folding one character can give several, such as a Hangul syllable's letters, so this grows as it goes.
    private var sources = IntArray(source.length)
    private var count = 0
    val folded: String

    init {
        val builder = StringBuilder(source.length)
        fun append(index: Int, folded: Char) {
            if (count == sources.size) sources = sources.copyOf(sources.size * 2 + 1)
            sources[count++] = index
            builder.append(folded)
        }
        source.forEachIndexed { index, character ->
            when {
                character.isWhitespace() -> append(index, ' ')
                character.code < ASCII_END -> append(index, character.lowercaseChar())
                // Lower-casing first, since it can itself add a mark, as it does to a dotted capital I.
                else -> Normalizer.normalize(character.toString().lowercase(), Normalizer.Form.NFD).forEach {
                    if (Character.getType(it) != Character.NON_SPACING_MARK.toInt()) append(index, it)
                }
            }
        }
        folded = builder.toString()
    }

    /** Where folded character [index] came from; the end of [source] past the last one. */
    fun sourceOffset(index: Int): Int = if (index < count) sources[index] else source.length
}

private const val ASCII_END = 0x80
private const val EXCERPT_BEFORE = 40
private const val EXCERPT_AFTER = 80
private const val ELLIPSIS = "…"
