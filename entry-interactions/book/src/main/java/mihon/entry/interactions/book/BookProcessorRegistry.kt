package mihon.entry.interactions.book

import mihon.book.api.BookContentDescriptor

internal class BookProcessorRegistry(
    processors: Collection<BookProcessor>,
) {
    private val processors = processors.associateBy(BookProcessor::id)

    init {
        require(this.processors.size == processors.size) {
            "Duplicate BOOK processor IDs: ${processors.groupingBy(BookProcessor::id).eachCount().duplicates()}"
        }
        require(this.processors.keys.none(String::isBlank)) { "BOOK processor IDs must not be blank" }
        require(this.processors.values.none { it.displayName.isBlank() }) { "BOOK processor names must not be blank" }
    }

    fun select(
        descriptor: BookContentDescriptor,
        rememberedProcessorId: String? = null,
    ): BookProcessorSelection {
        val candidates = processors.values.filter { it.supports(descriptor) }
        if (candidates.isEmpty()) return BookProcessorSelection.Unsupported

        val remembered = rememberedProcessorId?.let(processors::get)?.takeIf { it in candidates }
        if (remembered != null) return BookProcessorSelection.Selected(remembered)
        if (candidates.size == 1) return BookProcessorSelection.Selected(candidates.single())

        return BookProcessorSelection.ChoiceRequired(candidates)
    }
}

internal sealed interface BookProcessorSelection {
    data object Unsupported : BookProcessorSelection
    data class Selected(val processor: BookProcessor) : BookProcessorSelection
    data class ChoiceRequired(val processors: List<BookProcessor>) : BookProcessorSelection
}

private fun Map<String, Int>.duplicates(): String = entries
    .filter { it.value > 1 }
    .joinToString { it.key }
