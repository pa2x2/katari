package mihon.entry.interactions.book.document.reader.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import mihon.entry.interactions.book.document.reader.BookDocumentSection
import tachiyomi.domain.entry.model.EntryChapter

/**
 * Finds the open chapter's matches whenever the query or the chapter changes, a moment after typing stops, and shows
 * the match the search picks.
 */
@Composable
internal fun BookDocumentChapterSearchMatching(
    search: BookDocumentChapterSearch,
    chapterId: Long,
    sections: List<BookDocumentSection<EntryChapter>>?,
    onReveal: (BookDocumentSearchMatch) -> Unit,
) {
    val query = search.query ?: return
    val currentOnReveal by rememberUpdatedState(onReveal)
    LaunchedEffect(query, chapterId, sections) {
        delay(TYPING_PAUSE_MILLIS)
        val found = withContext(Dispatchers.Default) {
            sections.orEmpty().flatMap { section ->
                val document = section.document.document
                bookDocumentTextMatches(document.content.text, query).map {
                    BookDocumentSearchMatch(section.key, document.resourceId, it)
                }
            }
        }
        search.found(query, chapterId, sections.orEmpty().map { it.key }, found)?.let(currentOnReveal)
    }
}

private const val TYPING_PAUSE_MILLIS = 200L
