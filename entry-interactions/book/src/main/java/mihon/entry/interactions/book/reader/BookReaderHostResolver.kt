package mihon.entry.interactions.book

import mihon.book.api.BookContentDescriptor
import mihon.book.api.BookFailure
import mihon.book.api.BookFailureReason
import mihon.book.api.model.BookPublicationModelDescriptor

internal class BookReaderHostResolver(
    private val sessionFactory: BookReaderSessionFactory,
    private val preparerRegistry: BookContentPreparerRegistry,
    private val selectionCoordinator: BookReaderProcessorSelectionCoordinator,
) {
    suspend fun resolve(entryId: Long, chapterId: Long): BookReaderHostState {
        val request = BookReaderRequest(entryId, chapterId)
        val prepared = when (val result = sessionFactory.prepare(request)) {
            is BookReaderPrepareResult.Failure -> return BookReaderHostState.Unavailable(result.failure)
            is BookReaderPrepareResult.Success -> result.request
        }
        val descriptor = prepared.content.descriptor
        val model = when (val preparation = preparerRegistry.resolve(descriptor)) {
            BookContentPreparerSelection.Unsupported -> {
                return BookReaderHostState.Unavailable(
                    failure = BookFailure(
                        reason = BookFailureReason.PROCESSOR_UNAVAILABLE,
                        message = descriptor.unsupportedMessage("No compatible content preparer is installed for"),
                    ),
                    descriptor = descriptor,
                )
            }
            is BookContentPreparerSelection.Ambiguous -> {
                return BookReaderHostState.Unavailable(
                    failure = BookFailure(
                        reason = BookFailureReason.PROCESSOR_UNAVAILABLE,
                        message = "Multiple content preparers claim this publication: " +
                            preparation.preparers.joinToString { it.id },
                    ),
                    descriptor = descriptor,
                )
            }
            is BookContentPreparerSelection.Selected -> preparation.preparer.outputModel
        }

        return when (val selection = selectionCoordinator.resolve(model)) {
            BookReaderProcessorSelection.Unsupported -> BookReaderHostState.Unavailable(
                failure = BookFailure(
                    reason = BookFailureReason.PROCESSOR_UNAVAILABLE,
                    message = descriptor.unsupportedMessage("No compatible reader is installed for"),
                ),
                descriptor = descriptor,
            )
            is BookReaderProcessorSelection.ChoiceRequired -> BookReaderHostState.ChoiceRequired(
                descriptor = descriptor,
                model = model,
                choices = selection.processors.map { BookProcessorChoice(it.id, it.displayName) },
                prepared = prepared,
            )
            is BookReaderProcessorSelection.Selected -> BookReaderHostState.ReaderSelected(
                descriptor = descriptor,
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
        val selected = selectionCoordinator.choose(state.model, processorId, remember)
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
        val model: BookPublicationModelDescriptor,
        val choices: List<BookProcessorChoice>,
        val prepared: PreparedBookReaderRequest,
    ) : BookReaderHostState

    data class ReaderSelected(
        val descriptor: BookContentDescriptor,
        val processor: BookReaderProcessor,
        val prepared: PreparedBookReaderRequest,
    ) : BookReaderHostState
}

internal data class BookProcessorChoice(val id: String, val displayName: String)

private fun BookContentDescriptor.unsupportedMessage(prefix: String): String = buildString {
    append(prefix)
    append(" format ")
    append(format)
    profile?.let { append(" (profile: $it)") }
    if (protection != "none") append(" with protection $protection")
    append('.')
}
