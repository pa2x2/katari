package mihon.entry.interactions.book

import mihon.book.api.BookContentDescriptor

internal class BookProcessorSelectionCoordinator(
    private val registry: BookProcessorRegistry,
    private val preferences: BookProcessorPreferences,
) {
    fun resolve(descriptor: BookContentDescriptor): BookProcessorSelection {
        val rememberedId = preferences.rememberedProcessorId(descriptor)
        val selection = registry.select(descriptor, rememberedId)
        if (
            rememberedId != null &&
            (selection !is BookProcessorSelection.Selected || selection.processor.id != rememberedId)
        ) {
            preferences.forget(descriptor)
        }
        return selection
    }

    fun choose(
        descriptor: BookContentDescriptor,
        processorId: String,
        remember: Boolean,
    ): BookProcessorSelection.Selected {
        val processor = registry.compatibleProcessors(descriptor)
            .firstOrNull { it.id == processorId }
            ?: throw IllegalArgumentException("BOOK processor $processorId is not compatible with this content")
        if (remember) preferences.remember(descriptor, processor.id)
        return BookProcessorSelection.Selected(processor)
    }
}
