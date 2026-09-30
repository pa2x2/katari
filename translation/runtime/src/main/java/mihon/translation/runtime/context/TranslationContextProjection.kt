package mihon.translation.runtime.context

import mihon.translation.api.request.TranslationContext
import mihon.translation.spi.engine.ContextualTranslationEngine
import mihon.translation.spi.engine.TranslationContextElement
import mihon.translation.spi.engine.TranslationEngine

/** The part of [context] that [engine] reads, which is all it is given and all its translations depend on. */
internal fun TranslationContext.readBy(engine: TranslationEngine): TranslationContext {
    val elements = (engine as? ContextualTranslationEngine)?.contextSupport.orEmpty()
    return TranslationContext(
        work = work.takeIf { TranslationContextElement.Work in elements },
        precedingText = precedingText.takeIf { TranslationContextElement.PrecedingText in elements }.orEmpty(),
    )
}

/**
 * Everything besides a text itself that shaped its translation: the context the engine read and the [segments] it
 * translated the text together with. Empty when nothing did, as with every engine that reads no context.
 */
internal fun TranslationContext.influences(segments: List<String> = emptyList()): List<String> = buildList {
    work?.let { work ->
        add("work")
        add(work.title)
        add(work.description.orEmpty())
    }
    if (precedingText.isNotEmpty()) {
        add("preceding:${precedingText.size}")
        addAll(precedingText)
    }
    if (segments.size > 1) {
        add("together:${segments.size}")
        addAll(segments)
    }
}
