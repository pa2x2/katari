package mihon.entry.interactions.book

import mihon.book.api.BookContentDescriptor
import mihon.book.api.BookFailure
import mihon.book.api.BookFailureReason

internal class BookReaderHostResolver(
    private val sessionFactory: BookReaderSessionFactory,
    private val selectionCoordinator: BookProcessorSelectionCoordinator,
) {
    suspend fun resolve(entryId: Long, chapterId: Long): BookReaderHostState {
        val request = BookReaderRequest(entryId, chapterId)
        val prepared = when (val result = sessionFactory.prepare(request)) {
            is BookReaderPrepareResult.Failure -> return BookReaderHostState.Unavailable(result.failure)
            is BookReaderPrepareResult.Success -> result.request
        }
        val media = prepared.media

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
                prepared = prepared,
            )
            is BookProcessorSelection.Selected -> BookReaderHostState.ReaderSelected(
                descriptor = media.descriptor,
                processor = selection.processor,
                prepared = prepared,
            )
        }
    }

    fun choose(
        state: BookReaderHostState.ChoiceRequired,
        processorId: String,
        remember: Boolean,
    ): BookReaderHostState.ReaderSelected {
        val selected = selectionCoordinator.choose(state.descriptor, processorId, remember)
        return BookReaderHostState.ReaderSelected(
            descriptor = state.descriptor,
            processor = selected.processor,
            prepared = state.prepared,
        )
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
        val prepared: PreparedBookReaderRequest,
    ) : BookReaderHostState

    data class ReaderSelected(
        val descriptor: BookContentDescriptor,
        val processor: BookProcessor,
        val prepared: PreparedBookReaderRequest,
    ) : BookReaderHostState
}

internal data class BookProcessorChoice(val id: String, val displayName: String)

private fun BookContentDescriptor.unsupportedMessage(): String = buildString {
    append("No compatible reader is installed for format ")
    append(format)
    profile?.let { append(" (profile: $it)") }
    if (protection != "none") append(" with protection $protection")
    append('.')
}
