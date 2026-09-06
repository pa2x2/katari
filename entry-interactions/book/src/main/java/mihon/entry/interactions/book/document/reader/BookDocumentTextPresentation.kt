package mihon.entry.interactions.book.document.reader

import mihon.book.api.document.BookDocumentInlineStyleRange
import mihon.book.api.document.BookDocumentTextDirection

/** Display-only bidi isolates, with source offsets retained for links, styles and clean selection text. */
internal class BookDocumentTextPresentation(
    val text: String,
    val insertedOffsets: Set<Int> = emptySet(),
    private val starts: IntArray? = null,
    private val ends: IntArray? = null,
) {
    fun start(offset: Int): Int = starts?.get(offset.coerceIn(starts.indices)) ?: offset.coerceIn(0, text.length)
    fun end(offset: Int): Int = ends?.get(offset.coerceIn(ends.indices)) ?: offset.coerceIn(0, text.length)
}

internal fun bookDocumentTextPresentation(
    text: String,
    styles: List<BookDocumentInlineStyleRange>,
): BookDocumentTextPresentation {
    val directions = styles.withIndex().filter { it.value.style.direction != null }
    if (directions.isEmpty()) return BookDocumentTextPresentation(text)
    val starts = IntArray(text.length + 1)
    val ends = IntArray(text.length + 1)
    val inserted = linkedSetOf<Int>()
    val display = StringBuilder()
    var active = emptyList<IndexedValue<BookDocumentInlineStyleRange>>()
    val boundaries = directions.flatMapTo(mutableSetOf(0, text.length)) {
        listOf(it.value.start.coerceIn(0, text.length), it.value.endExclusive.coerceIn(0, text.length))
    }
    text.forEachIndexed { index, character ->
        if (character == '\n' || character == '\r' || character == '\u2029') {
            boundaries += index
            boundaries += index + 1
        }
    }
    fun insert(character: Char) {
        inserted += display.length
        display.append(character)
    }
    for (offset in 0..text.length) {
        ends[offset] = display.length
        if (offset in boundaries) {
            // Outer spans open first. Parser ranges are emitted child-first, including coincident bounds.
            val paragraphBreak = when (text.getOrNull(offset)) {
                '\n', '\r', '\u2029' -> true
                else -> false
            }
            val next = directions.filter {
                !paragraphBreak && offset >= it.value.start &&
                    offset < it.value.endExclusive.coerceAtMost(text.length)
            }
                .sortedWith(
                    compareBy<IndexedValue<BookDocumentInlineStyleRange>> { it.value.start }
                        .thenByDescending { it.value.endExclusive }.thenByDescending { it.index },
                )
            val shared = active.zip(next).takeWhile { (old, new) -> old.index == new.index }.size
            repeat(active.size - shared) { insert('\u2069') }
            next.drop(shared).forEach {
                insert(if (it.value.style.direction == BookDocumentTextDirection.RIGHT_TO_LEFT) '\u2067' else '\u2066')
            }
            active = next
        }
        starts[offset] = display.length
        if (offset < text.length) display.append(text[offset])
    }
    return BookDocumentTextPresentation(display.toString(), inserted, starts, ends)
}
