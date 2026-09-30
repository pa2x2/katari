package mihon.translation.runtime.batch

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import mihon.translation.api.preparation.TranslationRequirement
import mihon.translation.api.provider.TranslationProviderOutputMode
import mihon.translation.api.request.ResolvedTranslationRequest
import mihon.translation.api.request.ResolvedTranslationRoute
import mihon.translation.api.request.TranslationBatch
import mihon.translation.api.result.TranslationBatchUpdate
import mihon.translation.runtime.cache.TranslationResultCache
import mihon.translation.runtime.context.influences
import mihon.translation.runtime.context.readBy
import mihon.translation.spi.engine.ContextualTranslationEngine
import mihon.translation.spi.engine.ReadyTranslationEngineRequest
import mihon.translation.spi.engine.TranslationContextElement
import mihon.translation.spi.engine.TranslationEngine
import mihon.translation.spi.engine.TranslationEngineBatchExecution
import mihon.translation.spi.engine.TranslationEngineExecution
import mihon.translation.spi.engine.TranslationEngineRequirement

/**
 * Translates the segments of a batch with an engine that is ready for the batch's route.
 *
 * A contextual engine is given as many segments per call as it takes, along with the context it reads and the
 * segments of earlier calls; any other engine translates each segment on its own. A translation is reused when
 * nothing that shaped it differs.
 */
internal class TranslationBatchExecutor(
    private val resultCache: TranslationResultCache?,
    private val ioDispatcher: CoroutineDispatcher,
) {
    /**
     * @param maximumCodePoints the longest segment, and the most text per call, [engine] accepts.
     * @param requirement describes what the user must resolve when [engine] stops being ready.
     */
    fun execute(
        engine: TranslationEngine,
        ready: ReadyTranslationEngineRequest,
        batch: TranslationBatch,
        maximumCodePoints: Int,
        requirement: (TranslationEngineRequirement) -> TranslationRequirement,
    ): Flow<TranslationBatchUpdate> = flow {
        val inline = engine.presentation.outputMode == TranslationProviderOutputMode.InlineResult
        val segments = batch.segments.withIndex().filter { (index, text) ->
            val accepted = inline && text.isNotBlank() && text.codePointLength() <= maximumCodePoints
            if (!accepted) emit(TranslationBatchUpdate.Failed(index))
            accepted
        }
        if (engine is ContextualTranslationEngine) {
            translateTogether(engine, ready, batch, segments, maximumCodePoints, requirement)
        } else {
            translateSeparately(engine, ready, batch.route, segments, requirement)
        }
    }

    private suspend fun FlowCollector<TranslationBatchUpdate>.translateSeparately(
        engine: TranslationEngine,
        ready: ReadyTranslationEngineRequest,
        route: ResolvedTranslationRoute,
        segments: List<IndexedValue<String>>,
        requirement: (TranslationEngineRequirement) -> TranslationRequirement,
    ) {
        for ((index, text) in segments) {
            val request = route.request(text)
            val cached = cached(request)
            if (cached != null) {
                emit(TranslationBatchUpdate.Translated(index, cached))
                continue
            }
            when (val execution = engine.translate(ready, text)) {
                is TranslationEngineExecution.Success -> {
                    remember(request, execution.translatedText)
                    emit(TranslationBatchUpdate.Translated(index, execution.translatedText))
                }
                is TranslationEngineExecution.PreparationChanged -> {
                    val changed = execution.preparation
                    if (changed is TranslationEngineRequirement) {
                        emit(TranslationBatchUpdate.Blocked(requirement(changed)))
                        return
                    }
                    emit(TranslationBatchUpdate.Failed(index))
                }
                TranslationEngineExecution.ProviderSurfaceOpened,
                is TranslationEngineExecution.Failed,
                -> emit(TranslationBatchUpdate.Failed(index))
            }
        }
    }

    private suspend fun FlowCollector<TranslationBatchUpdate>.translateTogether(
        engine: ContextualTranslationEngine,
        ready: ReadyTranslationEngineRequest,
        batch: TranslationBatch,
        segments: List<IndexedValue<String>>,
        maximumCodePoints: Int,
        requirement: (TranslationEngineRequirement) -> TranslationRequirement,
    ) {
        var context = batch.context.readBy(engine)
        for (call in segments.calls(engine.maximumBatchSegments, maximumCodePoints)) {
            val texts = call.map { it.value }
            val requests = texts.map { batch.route.request(it) }
            val influences = context.influences(texts)
            val cached = requests.map { cached(it, influences) }
            val translations = if (cached.none { it == null }) {
                cached
            } else {
                when (val execution = engine.translate(ready, texts, context)) {
                    is TranslationEngineBatchExecution.Success -> {
                        // Translations that do not answer every segment cannot be told apart, so none is kept.
                        val answered = execution.translatedTexts.takeIf { it.size == texts.size }.orEmpty()
                        texts.indices.map { position ->
                            answered.getOrNull(position)?.takeIf(String::isNotBlank)
                                ?.also { remember(requests[position], it, influences) }
                        }
                    }
                    is TranslationEngineBatchExecution.PreparationChanged -> {
                        val changed = execution.preparation
                        if (changed is TranslationEngineRequirement) {
                            emit(TranslationBatchUpdate.Blocked(requirement(changed)))
                            return
                        }
                        texts.map { null }
                    }
                    is TranslationEngineBatchExecution.Failed -> texts.map { null }
                }
            }
            call.forEachIndexed { position, segment ->
                emit(
                    translations[position]?.let { TranslationBatchUpdate.Translated(segment.index, it) }
                        ?: TranslationBatchUpdate.Failed(segment.index),
                )
            }
            if (TranslationContextElement.PrecedingText in engine.contextSupport) {
                context = context.copy(precedingText = context.precedingText + texts)
            }
        }
    }

    /** Consecutive segments grouped into as few engine calls as [maximumSegments] and [maximumCodePoints] allow. */
    private fun List<IndexedValue<String>>.calls(
        maximumSegments: Int,
        maximumCodePoints: Int,
    ): List<List<IndexedValue<String>>> {
        val calls = mutableListOf<MutableList<IndexedValue<String>>>()
        var codePoints = 0
        forEach { segment ->
            val length = segment.value.codePointLength()
            val current = calls.lastOrNull()
            if (current == null || current.size >= maximumSegments || codePoints + length > maximumCodePoints) {
                calls += mutableListOf(segment)
                codePoints = length
            } else {
                current += segment
                codePoints += length
            }
        }
        return calls
    }

    private suspend fun cached(request: ResolvedTranslationRequest, influences: List<String> = emptyList()): String? =
        resultCache?.let { cache -> withContext(ioDispatcher) { cache.get(request, influences) } }

    private suspend fun remember(
        request: ResolvedTranslationRequest,
        translatedText: String,
        influences: List<String> = emptyList(),
    ) {
        resultCache?.let { cache -> withContext(ioDispatcher) { cache.put(request, translatedText, influences) } }
    }

    private fun ResolvedTranslationRoute.request(text: String) =
        ResolvedTranslationRequest(text, sourceLanguage, targetLanguage, engine)

    private fun String.codePointLength(): Int = codePointCount(0, length)
}
