package mihon.entry.interactions.book.document.reader.search

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import mihon.book.api.document.locatorAt
import mihon.entry.interactions.book.document.reader.BookDocumentViewerLocation
import mihon.entry.interactions.book.document.reader.navigation.BookDocumentNavigationTarget
import tachiyomi.domain.entry.model.EntryChapter

/**
 * Search in the chapter being read. While [query] is set, the chapter's [matches] are marked and one of them is
 * current, which the reader shows. A new query starts from the first match after where reading was when the search
 * opened; closing goes back there, unless the reader moved to another chapter's result in the meantime.
 */
@Stable
internal class BookDocumentChapterSearch {
    /** Null while the search is closed. */
    var query by mutableStateOf<String?>(null)
        private set
    var matches by mutableStateOf<List<BookDocumentSearchMatch>>(emptyList())
        private set

    /** -1 while no match is current, such as after reading on into another chapter. */
    var currentIndex by mutableIntStateOf(-1)
        private set

    private var origin: BookDocumentNavigationTarget? = null

    /** Whether a match was shown, so closing has somewhere to go back from. */
    private var moved = false
    private var originSectionKey: String? = null
    private var originOffset = 0
    private var foundQuery: String? = null
    private var foundChapterId: Long? = null
    private var pendingResult: BookDocumentBookSearchResult? = null

    val current: BookDocumentSearchMatch? get() = matches.getOrNull(currentIndex)

    // Derived once per change, so text on the page sees the same marks until they really change.
    val marks: BookDocumentSearchMarks? by derivedStateOf {
        if (query.isNullOrBlank()) {
            null
        } else {
            BookDocumentSearchMarks(
                matches.groupBy(BookDocumentSearchMatch::sectionKey) { it.match },
                current,
            )
        }
    }

    fun open(location: BookDocumentViewerLocation<EntryChapter>?) {
        query = ""
        val document = location?.section?.document?.document
        origin = location?.let {
            BookDocumentNavigationTarget(it.section.owner, document?.locatorAt(it.position), restorePosition = true)
        }
        originSectionKey = location?.section?.key
        originOffset = location?.let { document?.logicalOffset(it.position) } ?: 0
    }

    fun changeQuery(value: String) {
        query = value
    }

    /**
     * Takes the open chapter's matches for [forQuery] and returns the one to show: the first after where reading was
     * for a new query, or a whole-book result the reader picked. A chapter read into with the same query shows none.
     */
    fun found(
        forQuery: String,
        chapterId: Long,
        sectionKeys: List<String>,
        chapterMatches: List<BookDocumentSearchMatch>,
    ): BookDocumentSearchMatch? {
        if (forQuery != query) return null
        matches = chapterMatches
        foundChapterId = chapterId
        val picked = pendingResult
        val index = when {
            picked != null -> chapterMatches.indexOfFirst(picked::locates).also { if (it >= 0) pendingResult = null }
            forQuery != foundQuery -> {
                val originSection = sectionKeys.indexOf(originSectionKey)
                chapterMatches.indexOfFirst {
                    val section = sectionKeys.indexOf(it.sectionKey)
                    section > originSection || (section == originSection && it.match.start >= originOffset)
                }.takeIf { it >= 0 } ?: if (chapterMatches.isEmpty()) -1 else 0
            }
            else -> -1
        }
        foundQuery = forQuery
        currentIndex = index
        return current?.also { moved = true }
    }

    /** Steps to the next match, or the previous one when [forward] is false, wrapping around the chapter. */
    fun step(forward: Boolean): BookDocumentSearchMatch? {
        if (matches.isEmpty()) return null
        currentIndex = when {
            currentIndex < 0 -> if (forward) 0 else matches.lastIndex
            forward -> (currentIndex + 1) % matches.size
            else -> (currentIndex - 1 + matches.size) % matches.size
        }
        moved = true
        return current
    }

    /**
     * Makes a whole-book [result] current: straight away when it is in the open chapter, and otherwise once its
     * chapter opens and its matches come in, so null means there is nothing to show yet. Closing then stays put.
     */
    fun showResult(result: BookDocumentBookSearchResult): BookDocumentSearchMatch? {
        origin = null
        if (result.chapter.id == foundChapterId) {
            val index = matches.indexOfFirst(result::locates)
            if (index >= 0) {
                currentIndex = index
                return current
            }
        }
        pendingResult = result
        return null
    }

    /** Closes the search and returns where to go back to, if anywhere. */
    fun close(): BookDocumentNavigationTarget? {
        val target = origin?.takeIf { moved }
        moved = false
        query = null
        matches = emptyList()
        currentIndex = -1
        origin = null
        foundQuery = null
        foundChapterId = null
        pendingResult = null
        return target
    }
}
