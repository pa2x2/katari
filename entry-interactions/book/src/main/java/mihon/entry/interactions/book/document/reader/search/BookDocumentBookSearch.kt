package mihon.entry.interactions.book.document.reader.search

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import logcat.LogPriority
import mihon.book.api.document.BookDocument
import mihon.book.api.document.locatorAt
import mihon.entry.interactions.book.document.preparation.PreparedBookDocumentPublication
import mihon.entry.interactions.book.document.reader.navigation.BookDocumentNavigationTarget
import mihon.entry.interactions.book.reader.OpenedBookReaderSession
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.model.EntryChapter

/** A match in [chapter]'s document [resourceId], with the text around it. */
internal data class BookDocumentBookSearchResult(
    val chapter: EntryChapter,
    val resourceId: String,
    val match: BookDocumentTextMatch,
    val excerpt: BookDocumentSearchExcerpt,
    val target: BookDocumentNavigationTarget,
) {
    fun locates(found: BookDocumentSearchMatch): Boolean = found.resourceId == resourceId && found.match == match
}

/** [results] lists the first of the chapter's [matchCount] matches. */
internal data class BookDocumentChapterSearchResults(
    val chapter: EntryChapter,
    val matchCount: Int,
    val results: List<BookDocumentBookSearchResult>,
)

/** How far a whole-book search has got: [searched] of the book's [total] chapters, and what it found in them. */
internal data class BookDocumentBookSearchProgress(
    val searched: Int,
    val total: Int,
    val chapters: List<BookDocumentChapterSearchResults>,
) {
    val done: Boolean get() = searched == total
    val matchCount: Int get() = chapters.sumOf(BookDocumentChapterSearchResults::matchCount)
}

/**
 * Searches every downloaded chapter of a book in reading order. Chapters the reader holds open are searched as they
 * are; the rest are opened from their downloads one at a time and closed again. Chapters that aren't downloaded are
 * passed over, so a search never reaches the source.
 */
internal class BookDocumentBookSearch(
    private val chapters: () -> List<EntryChapter>,
    private val openPublications: () -> Map<Long, PreparedBookDocumentPublication>,
    private val openDownloaded: suspend (EntryChapter) -> OpenedBookReaderSession?,
) {
    /** Call on the main thread, where the reader's open chapters are read; the search itself runs off it. */
    fun search(query: String): Flow<BookDocumentBookSearchProgress> {
        val chapters = chapters()
        val open = openPublications()
        return search(query, chapters, open)
    }

    private fun search(
        query: String,
        chapters: List<EntryChapter>,
        open: Map<Long, PreparedBookDocumentPublication>,
    ): Flow<BookDocumentBookSearchProgress> = flow {
        val found = mutableListOf<BookDocumentChapterSearchResults>()
        emit(BookDocumentBookSearchProgress(0, chapters.size, emptyList()))
        chapters.forEachIndexed { index, chapter ->
            searchChapter(chapter, query, open[chapter.id])?.let(found::add)
            emit(BookDocumentBookSearchProgress(index + 1, chapters.size, found.toList()))
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun searchChapter(
        chapter: EntryChapter,
        query: String,
        open: PreparedBookDocumentPublication?,
    ): BookDocumentChapterSearchResults? {
        open?.let { return it.search(chapter, query) }
        val session = try {
            openDownloaded(chapter)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            logcat(LogPriority.WARN, error) { "Failed to open a downloaded BOOK chapter for search" }
            null
        } ?: return null
        return session.use { (it.preparedPublication as? PreparedBookDocumentPublication)?.search(chapter, query) }
    }

    private fun PreparedBookDocumentPublication.search(
        chapter: EntryChapter,
        query: String,
    ): BookDocumentChapterSearchResults? {
        var matchCount = 0
        val results = buildList {
            documents.forEach { document ->
                val matches = bookDocumentTextMatches(document.content.text, query)
                matchCount += matches.size
                matches.take((RESULTS_PER_CHAPTER - size).coerceAtLeast(0)).forEach { match ->
                    add(document.result(chapter, match))
                }
            }
        }
        return BookDocumentChapterSearchResults(chapter, matchCount, results).takeIf { matchCount > 0 }
    }

    private fun BookDocument.result(chapter: EntryChapter, match: BookDocumentTextMatch) = BookDocumentBookSearchResult(
        chapter = chapter,
        resourceId = resourceId,
        match = match,
        excerpt = bookDocumentSearchExcerpt(content.text, match),
        target = BookDocumentNavigationTarget(
            chapter = chapter,
            locator = locatorAt(positionAtLogicalOffset(match.start)),
            restorePosition = true,
        ),
    )
}

// Enough to find a passage by; the count still tells how many more there are.
private const val RESULTS_PER_CHAPTER = 50
