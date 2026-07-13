package mihon.entry.interactions.book

import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.CancellationException
import mihon.book.api.BookContentDescriptor
import mihon.book.api.BookFailure
import mihon.book.api.BookFailureReason
import tachiyomi.domain.entry.adapter.toSEntryChapter
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.source.service.SourceManager

internal class BookReaderHostResolver(
    private val entryRepository: EntryRepository,
    private val entryChapterRepository: EntryChapterRepository,
    private val sourceManager: SourceManager,
    private val selectionCoordinator: BookProcessorSelectionCoordinator,
) {
    suspend fun resolve(entryId: Long, chapterId: Long): BookReaderHostState {
        val visibleEntry = entryRepository.getEntryById(entryId)
            ?: return unavailable(BookFailureReason.CONTENT_UNAVAILABLE, "The book entry no longer exists.")
        if (!visibleEntry.isBook()) {
            return unavailable(BookFailureReason.MALFORMED_CONTENT, "The selected entry is not a book.")
        }
        val chapter = entryChapterRepository.getChapterById(chapterId)
            ?: return unavailable(BookFailureReason.CONTENT_UNAVAILABLE, "The selected book item no longer exists.")
        val owner = entryRepository.getEntryById(chapter.entryId)
            ?: return unavailable(BookFailureReason.CONTENT_UNAVAILABLE, "The book item owner no longer exists.")
        if (!owner.isBook()) {
            return unavailable(BookFailureReason.MALFORMED_CONTENT, "The selected item does not belong to a book.")
        }
        val source = sourceManager.get(owner.source)
            ?: return unavailable(BookFailureReason.CONTENT_UNAVAILABLE, "The book source is not available.")
        val resolvedMedia = try {
            source.getMedia(chapter.toSourceChapter())
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            return unavailable(
                BookFailureReason.CONTENT_UNAVAILABLE,
                error.message ?: "The source could not resolve this book item.",
            )
        }
        val media = resolvedMedia as? EntryMedia.Book
            ?: return unavailable(
                BookFailureReason.MALFORMED_CONTENT,
                "The source returned a different content type for this book item.",
            )

        return when (val selection = selectionCoordinator.resolve(media.descriptor)) {
            BookProcessorSelection.Unsupported -> BookReaderHostState.Unavailable(
                failure = BookFailure(
                    reason = BookFailureReason.PROCESSOR_UNAVAILABLE,
                    message = media.descriptor.unsupportedMessage(),
                ),
                descriptor = media.descriptor,
            )
            is BookProcessorSelection.ChoiceRequired -> BookReaderHostState.ChoiceRequired(
                descriptor = media.descriptor,
                choices = selection.processors.map { BookProcessorChoice(it.id, it.displayName) },
            )
            is BookProcessorSelection.Selected -> BookReaderHostState.ReaderSelected(
                descriptor = media.descriptor,
                processorName = selection.processor.displayName,
            )
        }
    }

    fun choose(
        state: BookReaderHostState.ChoiceRequired,
        processorId: String,
        remember: Boolean,
    ): BookReaderHostState.ReaderSelected {
        val selected = selectionCoordinator.choose(state.descriptor, processorId, remember)
        return BookReaderHostState.ReaderSelected(state.descriptor, selected.processor.displayName)
    }

    private fun unavailable(reason: BookFailureReason, message: String): BookReaderHostState.Unavailable {
        return BookReaderHostState.Unavailable(BookFailure(reason, message))
    }
}

internal sealed interface BookReaderHostState {
    data class Unavailable(
        val failure: BookFailure,
        val descriptor: BookContentDescriptor? = null,
    ) : BookReaderHostState

    data class ChoiceRequired(
        val descriptor: BookContentDescriptor,
        val choices: List<BookProcessorChoice>,
    ) : BookReaderHostState

    data class ReaderSelected(
        val descriptor: BookContentDescriptor,
        val processorName: String,
    ) : BookReaderHostState
}

internal data class BookProcessorChoice(val id: String, val displayName: String)

private fun Entry.isBook(): Boolean = type == EntryType.BOOK

private fun EntryChapter.toSourceChapter() = toSEntryChapter()

private fun BookContentDescriptor.unsupportedMessage(): String = buildString {
    append("No compatible reader is installed for format ")
    append(format)
    profile?.let { append(" (profile: $it)") }
    if (protection != "none") append(" with protection $protection")
    append('.')
}
