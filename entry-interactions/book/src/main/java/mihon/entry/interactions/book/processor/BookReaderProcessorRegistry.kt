package mihon.entry.interactions.book.processor

import mihon.book.api.model.BookPublicationModelDescriptor
import mihon.entry.viewer.settings.shared.ReaderCapabilityId

internal class BookReaderProcessorRegistry(
    processors: Collection<BookReaderProcessor>,
) {
    private val processors = processors.associateBy(BookReaderProcessor::id)

    init {
        require(this.processors.size == processors.size) {
            "Duplicate BOOK reader processor IDs: " +
                processors.groupingBy(BookReaderProcessor::id).eachCount().duplicates()
        }
        require(this.processors.keys.none(String::isBlank)) { "BOOK reader processor IDs must not be blank" }
        require(this.processors.values.none { it.displayName.isBlank() }) {
            "BOOK reader processor names must not be blank"
        }
        require(this.processors.values.none { it.viewerSettingsSurfaceId?.isBlank() == true }) {
            "BOOK processor viewer settings surface IDs must not be blank"
        }
    }

    fun select(
        model: BookPublicationModelDescriptor,
        rememberedProcessorId: String? = null,
    ): BookReaderProcessorSelection {
        val candidates = compatibleProcessors(model)
        if (candidates.isEmpty()) return BookReaderProcessorSelection.Unsupported

        val remembered = rememberedProcessorId?.let(processors::get)?.takeIf { it in candidates }
        if (remembered != null) return BookReaderProcessorSelection.Selected(remembered)
        if (candidates.size == 1) return BookReaderProcessorSelection.Selected(candidates.single())

        return BookReaderProcessorSelection.ChoiceRequired(candidates)
    }

    fun compatibleProcessors(model: BookPublicationModelDescriptor): List<BookReaderProcessor> =
        processors.values.filter { it.supports(model) }

    fun get(processorId: String): BookReaderProcessor? = processors[processorId]

    fun potentialReaderCapabilities(): Set<ReaderCapabilityId> =
        processors.values.flatMapTo(mutableSetOf()) { it.potentialReaderCapabilities }

    fun potentialReaderCapabilitiesBySettingsSurface(): Map<String, Set<ReaderCapabilityId>> =
        processors.values
            .mapNotNull { processor ->
                processor.viewerSettingsSurfaceId?.let { surfaceId ->
                    surfaceId to processor.potentialReaderCapabilities
                }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, capabilitySets) -> capabilitySets.flatten().toSet() }
}

internal sealed interface BookReaderProcessorSelection {
    data object Unsupported : BookReaderProcessorSelection
    data class Selected(val processor: BookReaderProcessor) : BookReaderProcessorSelection
    data class ChoiceRequired(val processors: List<BookReaderProcessor>) : BookReaderProcessorSelection
}

private fun Map<String, Int>.duplicates(): String = entries
    .filter { it.value > 1 }
    .joinToString { it.key }
