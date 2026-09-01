package mihon.translation.runtime.feature

import mihon.language.api.identification.TextLanguageDetector
import mihon.language.api.tag.LanguageTag
import mihon.language.runtime.identification.AutomaticTextLanguageResolution
import mihon.language.runtime.identification.AutomaticTextLanguageResolver
import mihon.translation.api.TranslationFeature
import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.api.engine.TranslationEngineSelection
import mihon.translation.api.preparation.ReadyTranslation
import mihon.translation.api.preparation.TranslationEngineChoiceReason
import mihon.translation.api.preparation.TranslationPreparation
import mihon.translation.api.preparation.TranslationRejectionReason
import mihon.translation.api.preparation.TranslationTargetChoiceReason
import mihon.translation.api.provider.TranslationProviderOutputMode
import mihon.translation.api.provider.TranslationProviderPresentation
import mihon.translation.api.request.ResolvedTranslationRequest
import mihon.translation.api.request.TranslationRequest
import mihon.translation.api.request.TranslationSourceLanguageSelection
import mihon.translation.api.request.TranslationTargetLanguageSelection
import mihon.translation.api.result.TranslationExecution
import mihon.translation.api.result.TranslationFailureReason
import mihon.translation.api.result.TranslationResult
import mihon.translation.spi.engine.KnownTranslationEngineCatalog
import mihon.translation.spi.engine.ReadyTranslationEngineRequest
import mihon.translation.spi.engine.TranslationEngine
import mihon.translation.spi.engine.TranslationEngineExecution
import mihon.translation.spi.engine.TranslationEnginePreparation
import mihon.translation.spi.engine.TranslationEngineRegistry

fun interface TranslationDefaultTargetLanguageResolver {
    fun resolve(): LanguageTag?
}

class DefaultTranslationFeature(
    private val engineRegistry: TranslationEngineRegistry,
    private val knownEngineCatalog: KnownTranslationEngineCatalog,
    private val textLanguageDetectors: List<TextLanguageDetector>,
    private val defaultTargetLanguageResolver: TranslationDefaultTargetLanguageResolver,
    private val selectedEngine: suspend () -> TranslationEngineId?,
) : TranslationFeature {
    private val automaticLanguageResolver = AutomaticTextLanguageResolver(textLanguageDetectors)

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
        val targetLanguage = when (val selection = request.targetLanguage) {
            TranslationTargetLanguageSelection.Default -> defaultTargetLanguageResolver.resolve()
                ?: return TranslationPreparation.TargetLanguageRequired(
                    sourceLanguage = sourceLanguage,
                    reason = TranslationTargetChoiceReason.NoDefaultTarget,
                )

            is TranslationTargetLanguageSelection.Explicit -> selection.language
        }
        if (sourceLanguage == targetLanguage) {
            return TranslationPreparation.TargetLanguageRequired(
                sourceLanguage = sourceLanguage,
                reason = TranslationTargetChoiceReason.SourceEqualsTarget,
            )
        }

        val engine = when (val selection = request.engine) {
            TranslationEngineSelection.ProfileDefault -> selectedEngine()
                ?: return noEngineConfigured()
            is TranslationEngineSelection.Explicit -> selection.engine
        }
        return prepareExplicitly(
            request = request,
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            codePointCount = codePointCount,
            engineId = engine,
        )
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

        val refreshed = prepared.engine.revalidate(prepared.engineRequest)
        if (refreshed !is TranslationEnginePreparation.Ready) {
            return TranslationExecution.PreparationChanged(
                refreshed.toApi(prepared.engine, prepared.request),
            )
        }

        return when (val execution = prepared.engine.translate(refreshed.request)) {
            is TranslationEngineExecution.Success ->
                if (prepared.presentation.outputMode == TranslationProviderOutputMode.InlineResult) {
                    TranslationExecution.Success(
                        TranslationResult(
                            translatedText = execution.translatedText,
                            sourceLanguage = prepared.request.sourceLanguage,
                            targetLanguage = prepared.request.targetLanguage,
                            presentation = prepared.presentation,
                        ),
                    )
                } else {
                    invalidProviderOutput(prepared.request.engine)
                }

            is TranslationEngineExecution.PreparationChanged -> TranslationExecution.PreparationChanged(
                execution.preparation.toApi(prepared.engine, prepared.request),
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

    private suspend fun prepareExplicitly(
        request: TranslationRequest,
        sourceLanguage: LanguageTag,
        targetLanguage: LanguageTag,
        codePointCount: Int,
        engineId: TranslationEngineId,
    ): TranslationPreparation {
        val engine = engineRegistry.find(engineId) ?: return missingEngine(engineId)
        val maximumCodePoints = engine.effectiveMaximumInputCodePoints()
        if (codePointCount > maximumCodePoints) {
            return inputTooLarge(codePointCount, maximumCodePoints)
        }
        val resolvedRequest = request.resolve(sourceLanguage, targetLanguage, engine)
        return engine.prepare(resolvedRequest).toApi(engine, resolvedRequest)
    }

    private fun TranslationRequest.resolve(
        sourceLanguage: LanguageTag,
        targetLanguage: LanguageTag,
        engine: TranslationEngine,
    ) = ResolvedTranslationRequest(
        text = text,
        sourceLanguage = sourceLanguage,
        targetLanguage = targetLanguage,
        engine = engine.catalogEntry.id,
    )

    private fun TranslationEngine.effectiveMaximumInputCodePoints(): Int {
        return minOf(
            SHARED_MAXIMUM_CODE_POINTS,
            maximumInputCodePoints ?: SHARED_MAXIMUM_CODE_POINTS,
        )
    }

    private fun missingEngine(engine: TranslationEngineId): TranslationPreparation {
        return TranslationPreparation.EngineChoiceRequired(
            reason = TranslationEngineChoiceReason.SelectedEngineUnavailable(engine),
            engines = knownEngineCatalog.knownEngines,
        )
    }

    private fun noEngineConfigured(): TranslationPreparation {
        return TranslationPreparation.EngineChoiceRequired(
            reason = TranslationEngineChoiceReason.NoEngineConfigured,
            engines = knownEngineCatalog.knownEngines,
        )
    }

    private fun TranslationEnginePreparation.toApi(
        engine: TranslationEngine,
        request: ResolvedTranslationRequest,
    ): TranslationPreparation {
        return when (this) {
            is TranslationEnginePreparation.Ready -> TranslationPreparation.Ready(
                translation = RuntimeReadyTranslation(engine, this.request, request, engine.presentation),
                request = request,
                presentation = engine.presentation,
            )

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

    private data class RuntimeReadyTranslation(
        val engine: TranslationEngine,
        val engineRequest: ReadyTranslationEngineRequest,
        val request: ResolvedTranslationRequest,
        val presentation: TranslationProviderPresentation,
    ) : ReadyTranslation

    private companion object {
        const val SHARED_MAXIMUM_CODE_POINTS = 10_000
    }
}
