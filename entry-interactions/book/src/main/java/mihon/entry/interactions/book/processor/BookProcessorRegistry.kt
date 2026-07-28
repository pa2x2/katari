package mihon.entry.interactions.book

import mihon.book.api.BookContentDescriptor
import mihon.entry.viewer.settings.ReaderCapabilityId

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
        require(this.processors.values.none { it.viewerSettingsSurfaceId?.isBlank() == true }) {
            "BOOK processor viewer settings surface IDs must not be blank"
        }
    }

    fun select(
        descriptor: BookContentDescriptor,
        rememberedProcessorId: String? = null,
    ): BookProcessorSelection {
        val candidates = compatibleProcessors(descriptor)
        if (candidates.isEmpty()) return BookProcessorSelection.Unsupported

        val remembered = rememberedProcessorId?.let(processors::get)?.takeIf { it in candidates }
        if (remembered != null) return BookProcessorSelection.Selected(remembered)
        if (candidates.size == 1) return BookProcessorSelection.Selected(candidates.single())

        return BookProcessorSelection.ChoiceRequired(candidates)
    }

    fun compatibleProcessors(descriptor: BookContentDescriptor): List<BookProcessor> =
        processors.values.filter { it.supports(descriptor) }

    fun get(processorId: String): BookProcessor? = processors[processorId]

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

internal sealed interface BookProcessorSelection {
    data object Unsupported : BookProcessorSelection
    data class Selected(val processor: BookProcessor) : BookProcessorSelection
    data class ChoiceRequired(val processors: List<BookProcessor>) : BookProcessorSelection
}

private fun Map<String, Int>.duplicates(): String = entries
    .filter { it.value > 1 }
    .joinToString { it.key }
