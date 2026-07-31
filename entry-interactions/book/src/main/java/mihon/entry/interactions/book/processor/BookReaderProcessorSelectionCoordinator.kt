package mihon.entry.interactions.book

import mihon.book.api.model.BookPublicationModelDescriptor
internal class BookReaderProcessorSelectionCoordinator(
    private val registry: BookReaderProcessorRegistry,
    private val preferences: BookReaderProcessorPreferences,
) {
    fun resolve(model: BookPublicationModelDescriptor): BookReaderProcessorSelection {
        val rememberedId = preferences.rememberedProcessorId(model)
        val selection = registry.select(model, rememberedId)
        if (
            rememberedId != null &&
            (selection !is BookReaderProcessorSelection.Selected || selection.processor.id != rememberedId)
        ) {
            preferences.forget(model)
        }
        return selection
    }

    fun choose(
        model: BookPublicationModelDescriptor,
        processorId: String,
        remember: Boolean,
    ): BookReaderProcessorSelection.Selected {
        val processor = registry.compatibleProcessors(model)
            .firstOrNull { it.id == processorId }
            ?: throw IllegalArgumentException("BOOK processor $processorId is not compatible with this content")
        if (remember) preferences.remember(model, processor.id)
        return BookReaderProcessorSelection.Selected(processor)
    }
}
