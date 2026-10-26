package mihon.entry.interactions.book.reader.failure

import dev.zacsweers.metro.Inject
import mihon.entry.interactions.book.processor.BookReaderRequest
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository

/** The book and chapter a reader was asked to open, named on its failure screen's bar. */
internal data class BookReaderChapterHeading(val bookTitle: String, val chapterName: String?)

/**
 * Looks the names up again from the request, since a reader can fail before or after it has the entry and chapter,
 * and the failure itself doesn't carry them.
 */
@Inject
internal class BookReaderChapterHeadingLoader(
    private val entryRepository: EntryRepository,
    private val entryChapterRepository: EntryChapterRepository,
) {
    suspend fun load(request: BookReaderRequest): BookReaderChapterHeading? {
        val entry = entryRepository.getEntryById(request.entryId) ?: return null
        val chapter = entryChapterRepository.getChapterById(request.chapterId)
        return BookReaderChapterHeading(bookTitle = entry.displayTitle, chapterName = chapter?.name)
    }
}
