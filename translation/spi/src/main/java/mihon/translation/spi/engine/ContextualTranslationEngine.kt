package mihon.translation.spi.engine

import mihon.translation.api.request.TranslationContext

/**
 * An engine that translates several texts in one call, each in light of the others and of their context.
 *
 * Translations of such an engine depend on everything it was given, so they are only reused for the same texts in
 * the same context.
 */
interface ContextualTranslationEngine : TranslationEngine {
    /** The context elements this engine reads. No other element is ever passed to it. */
    val contextSupport: Set<TranslationContextElement>

    /** The most texts one call may carry. */
    val maximumBatchSegments: Int

    /** Translates [segments] together; a success answers each of them, in order. */
    suspend fun translate(
        ready: ReadyTranslationEngineRequest,
        segments: List<String>,
        context: TranslationContext,
    ): TranslationEngineBatchExecution

    override suspend fun translate(ready: ReadyTranslationEngineRequest, text: String): TranslationEngineExecution =
        translate(ready, text, TranslationContext.None)
}

/** Translates a single [text] in its [context]. */
suspend fun ContextualTranslationEngine.translate(
    ready: ReadyTranslationEngineRequest,
    text: String,
    context: TranslationContext,
): TranslationEngineExecution {
    return when (val execution = translate(ready, listOf(text), context)) {
        is TranslationEngineBatchExecution.Success -> execution.translatedTexts.singleOrNull()
            ?.takeIf(String::isNotBlank)
            ?.let(TranslationEngineExecution::Success)
            ?: TranslationEngineExecution.Failed()
        is TranslationEngineBatchExecution.PreparationChanged ->
            TranslationEngineExecution.PreparationChanged(execution.preparation)
        is TranslationEngineBatchExecution.Failed -> TranslationEngineExecution.Failed(execution.message)
    }
}

enum class TranslationContextElement {
    Work,
    PrecedingText,
}

sealed interface TranslationEngineBatchExecution {
    data class Success(
        val translatedTexts: List<String>,
    ) : TranslationEngineBatchExecution

    data class PreparationChanged(
        val preparation: TranslationEnginePreparation,
    ) : TranslationEngineBatchExecution

    data class Failed(
        val message: String? = null,
    ) : TranslationEngineBatchExecution
}
