package mihon.entry.interactions.book.document.reader.search

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

/** A match in the section of the open chapter keyed [sectionKey], whose document is [resourceId]. */
internal data class BookDocumentSearchMatch(
    val sectionKey: String,
    val resourceId: String,
    val match: BookDocumentTextMatch,
)

/** One mark to draw, relative to the start of the text it falls in. */
internal data class BookDocumentSearchMark(val start: Int, val endExclusive: Int, val current: Boolean)

/** The matches marked on the page, each section's sorted by where they start, and the one being shown. */
@Immutable
internal class BookDocumentSearchMarks(
    private val matches: Map<String, List<BookDocumentTextMatch>>,
    private val current: BookDocumentSearchMatch?,
) {
    /** The marks within [length] characters of [sectionKey]'s canonical text from [start]. */
    fun within(sectionKey: String, start: Int, length: Int): List<BookDocumentSearchMark> {
        val sectionMatches = matches[sectionKey] ?: return emptyList()
        val end = start + length
        // The first match ending after start; one starting earlier can still reach into this text.
        var index = sectionMatches.binarySearch { it.endExclusive.compareTo(start + 1) }
            .let { if (it < 0) -it - 1 else it }
        return buildList {
            while (index < sectionMatches.size && sectionMatches[index].start < end) {
                val match = sectionMatches[index++]
                add(
                    BookDocumentSearchMark(
                        start = (match.start - start).coerceAtLeast(0),
                        endExclusive = (match.endExclusive - start).coerceAtMost(length),
                        current = current?.sectionKey == sectionKey && current.match == match,
                    ),
                )
            }
        }
    }
}

internal val LocalBookDocumentSearchMarks = compositionLocalOf<BookDocumentSearchMarks?> { null }

/**
 * Where the content being drawn starts in its section's canonical text: zero for the document itself, and past the
 * block that holds it for a disclosure's body or a side-by-side item, whose blocks count from their own start.
 */
internal val LocalBookDocumentContentStart = staticCompositionLocalOf { 0 }
