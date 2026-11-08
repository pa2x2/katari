package mihon.translation.runtime.feature

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import mihon.language.api.identification.TextLanguageDetector
import mihon.language.api.tag.LanguageTag
import mihon.language.runtime.identification.AutomaticTextLanguageResolution
import mihon.language.runtime.identification.AutomaticTextLanguageResolver
import mihon.translation.api.TranslationFeature
import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.api.engine.TranslationEngineSelection
import mihon.translation.api.language.TranslationDefaultTarget
import mihon.translation.api.preparation.ReadyTranslation
import mihon.translation.api.preparation.TranslationEngineChoiceReason
import mihon.translation.api.preparation.TranslationPreparation
import mihon.translation.api.preparation.TranslationRejectionReason
import mihon.translation.api.preparation.TranslationRequirement
import mihon.translation.api.preparation.TranslationRoutePreparation
import mihon.translation.api.preparation.TranslationTargetChoiceReason
import mihon.translation.api.provider.TranslationProviderOutputMode
import mihon.translation.api.provider.TranslationProviderPresentation
import mihon.translation.api.request.ResolvedTranslationRequest
import mihon.translation.api.request.ResolvedTranslationRoute
import mihon.translation.api.request.TranslationBatch
import mihon.translation.api.request.TranslationContext
import mihon.translation.api.request.TranslationRequest
import mihon.translation.api.request.TranslationRouteRequest
import mihon.translation.api.request.TranslationSourceLanguageSelection
import mihon.translation.api.request.TranslationTargetLanguageSelection
import mihon.translation.api.result.TranslationBatchUpdate
import mihon.translation.api.result.TranslationExecution
import mihon.translation.api.result.TranslationFailureReason
import mihon.translation.api.result.TranslationResult
import mihon.translation.runtime.batch.TranslationBatchExecutor
import mihon.translation.runtime.cache.TranslationResultCache
import mihon.translation.runtime.context.influences
import mihon.translation.runtime.context.readBy
import mihon.translation.spi.engine.ContextualTranslationEngine
import mihon.translation.spi.engine.KnownTranslationEngineCatalog
import mihon.translation.spi.engine.ReadyTranslationEngineRequest
import mihon.translation.spi.engine.TranslationEngine
import mihon.translation.spi.engine.TranslationEngineExecution
import mihon.translation.spi.engine.TranslationEnginePreparation
import mihon.translation.spi.engine.TranslationEngineRegistry
import mihon.translation.spi.engine.TranslationEngineRequirement
import mihon.translation.spi.engine.translate

fun interface TranslationDefaultTargetLanguageResolver {
    fun resolve(): TranslationDefaultTarget?
}

class DefaultTranslationFeature(
    private val engineRegistry: TranslationEngineRegistry,
    private val knownEngineCatalog: KnownTranslationEngineCatalog,
    private val textLanguageDetectors: List<TextLanguageDetector>,
    private val defaultTargetLanguageResolver: TranslationDefaultTargetLanguageResolver,
    private val selectedEngine: suspend () -> TranslationEngineId?,
    private val resultCache: TranslationResultCache? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : TranslationFeature {
    private val automaticLanguageResolver = AutomaticTextLanguageResolver(textLanguageDetectors)
    private val batchExecutor = TranslationBatchExecutor(resultCache, ioDispatcher)

    override suspend fun prepare(request: TranslationRequest): TranslationPreparation {
        if (request.text.isBlank()) {
            return TranslationPreparation.Rejected(TranslationRejectionReason.BlankInput)
        }

        val codePointCount = request.text.codePointCount(0, request.text.length)
        if (codePointCount > SHARED_MAXIMUM_CODE_POINTS) {
            return inputTooLarge(codePointCount, SHARED_MAXIMUM_CODE_POINTS)
        }

        val sourceLanguage = when (val resolution = resolveSourceLanguage(request)) {
            is AutomaticTextLanguageResolution.Resolved -> resolution.language
            is AutomaticTextLanguageResolution.Undetermined -> {
                return TranslationPreparation.SourceUndetermined(resolution.suggestedLanguages)
            }
        }
        val (engine, route) = when (
            val resolution = resolveRoute(sourceLanguage, request.targetLanguage, request.engine)
        ) {
            is RouteResolution.Blocked -> return resolution.requirement
            is RouteResolution.Resolved -> resolution
        }
        val maximumCodePoints = engine.effectiveMaximumInputCodePoints()
        if (codePointCount > maximumCodePoints) {
            return inputTooLarge(codePointCount, maximumCodePoints)
        }
        val resolvedRequest = ResolvedTranslationRequest(
            text = request.text,
            sourceLanguage = route.sourceLanguage,
            targetLanguage = route.targetLanguage,
            engine = route.engine,
        )
        return engine.prepare(route).toApi(engine, resolvedRequest, request.context.readBy(engine))
    }

    override suspend fun prepareRoute(route: TranslationRouteRequest): TranslationRoutePreparation {
        val (engine, resolved) = when (
            val resolution = resolveRoute(route.sourceLanguage, route.targetLanguage, route.engine)
        ) {
            is RouteResolution.Blocked -> return resolution.requirement
            is RouteResolution.Resolved -> resolution
        }
        return when (val preparation = engine.prepare(resolved)) {
            is TranslationEnginePreparation.Ready -> TranslationRoutePreparation.Ready(resolved, engine.presentation)
            is TranslationEngineRequirement -> preparation.toApi(engine)
        }
    }

    override suspend fun translate(ready: ReadyTranslation): TranslationExecution {
        val prepared = ready as? RuntimeReadyTranslation
            ?: return TranslationExecution.Failed(TranslationFailureReason.InvalidReadyTranslation)
        val installed = engineRegistry.find(prepared.request.engine)
        if (installed !== prepared.engine) {
            return TranslationExecution.PreparationChanged(
                missingEngine(prepared.request.engine),
            )
        }

        val influences = prepared.context.influences()
        if (prepared.presentation.outputMode == TranslationProviderOutputMode.InlineResult) {
            cached(prepared.request, influences)?.let { translatedText ->
                return TranslationExecution.Success(prepared.result(translatedText))
            }
        }

        val refreshed = prepared.engine.revalidate(prepared.engineRequest)
        if (refreshed !is TranslationEnginePreparation.Ready) {
            return TranslationExecution.PreparationChanged(
                refreshed.toApi(prepared.engine, prepared.request, prepared.context),
            )
        }

        val engine = prepared.engine
        val execution = if (engine is ContextualTranslationEngine) {
            engine.translate(refreshed.request, prepared.request.text, prepared.context)
        } else {
            engine.translate(refreshed.request, prepared.request.text)
        }
        return when (execution) {
            is TranslationEngineExecution.Success ->
                if (prepared.presentation.outputMode == TranslationProviderOutputMode.InlineResult) {
                    remember(prepared.request, execution.translatedText, influences)
                    TranslationExecution.Success(prepared.result(execution.translatedText))
                } else {
                    invalidProviderOutput(prepared.request.engine)
                }

            is TranslationEngineExecution.PreparationChanged -> TranslationExecution.PreparationChanged(
                execution.preparation.toApi(prepared.engine, prepared.request, prepared.context),
            )

            TranslationEngineExecution.ProviderSurfaceOpened ->
                if (prepared.presentation.outputMode == TranslationProviderOutputMode.ProviderSurface) {
                    TranslationExecution.ProviderSurfaceOpened(prepared.presentation)
                } else {
                    invalidProviderOutput(prepared.request.engine)
                }

            is TranslationEngineExecution.Failed -> TranslationExecution.Failed(
                TranslationFailureReason.ProviderFailure(
                    engine = prepared.request.engine,
                    message = execution.message,
                ),
            )
        }
    }

    override fun translateBatch(batch: TranslationBatch): Flow<TranslationBatchUpdate> = flow {
        val engine = engineRegistry.find(batch.route.engine)
            ?: return@flow emit(TranslationBatchUpdate.Blocked(missingEngine(batch.route.engine)))
        val ready = when (val preparation = engine.prepare(batch.route)) {
            is TranslationEnginePreparation.Ready -> preparation.request
            is TranslationEngineRequirement ->
                return@flow emit(TranslationBatchUpdate.Blocked(preparation.toApi(engine)))
        }
        emitAll(
            batchExecutor.execute(
                engine = engine,
                ready = ready,
                batch = batch,
                maximumCodePoints = engine.effectiveMaximumInputCodePoints(),
                requirement = { it.toApi(engine) },
            ),
        )
    }

    private suspend fun resolveSourceLanguage(request: TranslationRequest): AutomaticTextLanguageResolution {
        return when (val selection = request.sourceLanguage) {
            is TranslationSourceLanguageSelection.Explicit ->
                AutomaticTextLanguageResolution.Resolved(selection.language)
            TranslationSourceLanguageSelection.Automatic -> automaticLanguageResolver.resolve(
                text = request.text,
                context = request.languageContext,
            )
        }
    }

    /** Settles the target and engine for text in [sourceLanguage]; nothing here depends on the text itself. */
    private suspend fun resolveRoute(
        sourceLanguage: LanguageTag,
        targetSelection: TranslationTargetLanguageSelection,
        engineSelection: TranslationEngineSelection,
    ): RouteResolution {
        val targetLanguage = when (targetSelection) {
            TranslationTargetLanguageSelection.Default -> defaultTargetLanguageResolver.resolve()?.language
                ?: return RouteResolution.Blocked(
                    TranslationPreparation.TargetLanguageRequired(
                        sourceLanguage = sourceLanguage,
                        reason = TranslationTargetChoiceReason.NoDefaultTarget,
                    ),
                )

            is TranslationTargetLanguageSelection.Explicit -> targetSelection.language
        }
        if (sourceLanguage == targetLanguage) {
            return RouteResolution.Blocked(
                TranslationPreparation.TargetLanguageRequired(
                    sourceLanguage = sourceLanguage,
                    reason = TranslationTargetChoiceReason.SourceEqualsTarget,
                ),
            )
        }

        val engineId = when (engineSelection) {
            TranslationEngineSelection.ProfileDefault -> selectedEngine()
                ?: return RouteResolution.Blocked(noEngineConfigured())
            is TranslationEngineSelection.Explicit -> engineSelection.engine
        }
        val engine = engineRegistry.find(engineId) ?: return RouteResolution.Blocked(missingEngine(engineId))
        return RouteResolution.Resolved(
            engine = engine,
            route = ResolvedTranslationRoute(sourceLanguage, targetLanguage, engine.catalogEntry.id),
        )
    }

    private fun TranslationEngine.effectiveMaximumInputCodePoints(): Int {
        return minOf(
            SHARED_MAXIMUM_CODE_POINTS,
            maximumInputCodePoints ?: SHARED_MAXIMUM_CODE_POINTS,
        )
    }

    private fun missingEngine(engine: TranslationEngineId): TranslationPreparation.EngineChoiceRequired {
        return TranslationPreparation.EngineChoiceRequired(
            reason = TranslationEngineChoiceReason.SelectedEngineUnavailable(engine),
            engines = knownEngineCatalog.knownEngines,
        )
    }

    private fun noEngineConfigured(): TranslationPreparation.EngineChoiceRequired {
        return TranslationPreparation.EngineChoiceRequired(
            reason = TranslationEngineChoiceReason.NoEngineConfigured,
            engines = knownEngineCatalog.knownEngines,
        )
    }

    private fun TranslationEnginePreparation.toApi(
        engine: TranslationEngine,
        request: ResolvedTranslationRequest,
        context: TranslationContext,
    ): TranslationPreparation {
        return when (this) {
            is TranslationEnginePreparation.Ready -> TranslationPreparation.Ready(
                translation = RuntimeReadyTranslation(engine, this.request, request, context, engine.presentation),
                request = request,
                presentation = engine.presentation,
            )

            is TranslationEngineRequirement -> toApi(engine)
        }
    }

    private fun TranslationEngineRequirement.toApi(engine: TranslationEngine): TranslationRequirement {
        return when (this) {
            is TranslationEnginePreparation.ProviderDisclosureRequired ->
                TranslationPreparation.ProviderDisclosureRequired(
                    engine = engine.catalogEntry.id,
                    presentation = engine.presentation,
                    disclosure = disclosure,
                )

            is TranslationEnginePreparation.ModelDownloadRequired -> TranslationPreparation.ModelDownloadRequired(
                engine = engine.catalogEntry.id,
                presentation = engine.presentation,
                models = models,
            )

            is TranslationEnginePreparation.SystemSetupRequired -> TranslationPreparation.SystemSetupRequired(
                engine = engine.catalogEntry.id,
                presentation = engine.presentation,
                reason = reason,
            )

            is TranslationEnginePreparation.SetupInProgress -> TranslationPreparation.SetupInProgress(
                engine = engine.catalogEntry.id,
                presentation = engine.presentation,
                progress = progress,
            )

            is TranslationEnginePreparation.Unavailable -> TranslationPreparation.Unavailable(reason)
        }
    }

    private fun inputTooLarge(actual: Int, maximum: Int): TranslationPreparation.Rejected {
        return TranslationPreparation.Rejected(
            TranslationRejectionReason.InputTooLarge(
                actualCodePoints = actual,
                maximumCodePoints = maximum,
            ),
        )
    }

    private fun invalidProviderOutput(engine: TranslationEngineId): TranslationExecution.Failed {
        return TranslationExecution.Failed(
            TranslationFailureReason.ProviderFailure(
                engine = engine,
                message = "Translation provider returned an incompatible output mode",
            ),
        )
    }

    private suspend fun cached(request: ResolvedTranslationRequest, influences: List<String>): String? =
        resultCache?.let { cache -> withContext(ioDispatcher) { cache.get(request, influences) } }

    private suspend fun remember(
        request: ResolvedTranslationRequest,
        translatedText: String,
        influences: List<String>,
    ) {
        resultCache?.let { cache -> withContext(ioDispatcher) { cache.put(request, translatedText, influences) } }
    }

    private fun RuntimeReadyTranslation.result(translatedText: String) = TranslationResult(
        translatedText = translatedText,
        sourceLanguage = request.sourceLanguage,
        targetLanguage = request.targetLanguage,
        presentation = presentation,
    )

    private sealed interface RouteResolution {
        data class Resolved(val engine: TranslationEngine, val route: ResolvedTranslationRoute) : RouteResolution

        class Blocked(val requirement: TranslationRequirement) : RouteResolution
    }

    private data class RuntimeReadyTranslation(
        val engine: TranslationEngine,
        val engineRequest: ReadyTranslationEngineRequest,
        val request: ResolvedTranslationRequest,
        /** The part of the request's context the engine reads. */
        val context: TranslationContext,
        val presentation: TranslationProviderPresentation,
    ) : ReadyTranslation

    private companion object {
        const val SHARED_MAXIMUM_CODE_POINTS = 10_000
    }
}
