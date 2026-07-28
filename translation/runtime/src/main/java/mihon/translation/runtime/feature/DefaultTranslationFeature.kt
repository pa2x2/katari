package mihon.translation.runtime

import mihon.translation.api.KnownTranslationEngine
import mihon.translation.api.ReadyTranslation
import mihon.translation.api.ResolvedTranslationRequest
import mihon.translation.api.TranslationEngineChoiceReason
import mihon.translation.api.TranslationEngineSelection
import mihon.translation.api.TranslationExecution
import mihon.translation.api.TranslationFailureReason
import mihon.translation.api.TranslationFeature
import mihon.translation.api.TranslationLanguageTag
import mihon.translation.api.TranslationPreparation
import mihon.translation.api.TranslationProviderPresentation
import mihon.translation.api.TranslationRejectionReason
import mihon.translation.api.TranslationRequest
import mihon.translation.api.TranslationResult
import mihon.translation.api.TranslationSourceLanguageSelection
import mihon.translation.api.TranslationTargetChoiceReason
import mihon.translation.api.TranslationTargetLanguageSelection
import mihon.translation.api.TranslationUnavailableReason
import mihon.translation.spi.KnownTranslationEngineCatalog
import mihon.translation.spi.ReadyTranslationEngineRequest
import mihon.translation.spi.TranslationEngine
import mihon.translation.spi.TranslationEngineExecution
import mihon.translation.spi.TranslationEnginePreparation
import mihon.translation.spi.TranslationEngineRegistry
import mihon.translation.spi.TranslationSourceLanguageDetection
import mihon.translation.spi.TranslationSourceLanguageDetector

fun interface TranslationDefaultTargetLanguageResolver {
    fun resolve(): TranslationLanguageTag?
}

class DefaultTranslationFeature(
    private val engineRegistry: TranslationEngineRegistry,
    private val knownEngineCatalog: KnownTranslationEngineCatalog,
    private val sourceLanguageDetectors: List<TranslationSourceLanguageDetector>,
    private val defaultTargetLanguageResolver: TranslationDefaultTargetLanguageResolver,
) : TranslationFeature {
    override suspend fun prepare(request: TranslationRequest): TranslationPreparation {
        if (request.text.isBlank()) {
            return TranslationPreparation.Rejected(TranslationRejectionReason.BlankInput)
        }

        val codePointCount = request.text.codePointCount(0, request.text.length)
        if (codePointCount > SHARED_MAXIMUM_CODE_POINTS) {
            return inputTooLarge(codePointCount, SHARED_MAXIMUM_CODE_POINTS)
        }

        val sourceLanguage = resolveSourceLanguage(request)
            ?: return TranslationPreparation.SourceUndetermined()
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

        val engine = resolveEngine(request.engine) ?: return missingEngine(request.engine)
        val maximumCodePoints = minOf(
            SHARED_MAXIMUM_CODE_POINTS,
            engine.maximumInputCodePoints ?: SHARED_MAXIMUM_CODE_POINTS,
        )
        if (codePointCount > maximumCodePoints) {
            return inputTooLarge(codePointCount, maximumCodePoints)
        }

        val resolvedRequest = ResolvedTranslationRequest(
            text = request.text,
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            engine = engine.catalogEntry.id,
        )
        return engine.prepare(resolvedRequest).toApi(engine, resolvedRequest)
    }

    override suspend fun translate(ready: ReadyTranslation): TranslationExecution {
        val prepared = ready as? RuntimeReadyTranslation
            ?: return TranslationExecution.Failed(TranslationFailureReason.InvalidReadyTranslation)
        val installed = engineRegistry.find(prepared.request.engine)
        if (installed !== prepared.engine) {
            return TranslationExecution.PreparationChanged(
                missingEngine(TranslationEngineSelection.Explicit(prepared.request.engine)),
            )
        }

        return when (val execution = prepared.engine.translate(prepared.engineRequest)) {
            is TranslationEngineExecution.Success -> TranslationExecution.Success(
                TranslationResult(
                    translatedText = execution.translatedText,
                    sourceLanguage = prepared.request.sourceLanguage,
                    targetLanguage = prepared.request.targetLanguage,
                    presentation = prepared.presentation,
                ),
            )

            is TranslationEngineExecution.PreparationChanged -> TranslationExecution.PreparationChanged(
                execution.preparation.toApi(prepared.engine, prepared.request),
            )

            is TranslationEngineExecution.Failed -> TranslationExecution.Failed(
                TranslationFailureReason.ProviderFailure(
                    engine = prepared.request.engine,
                    message = execution.message,
                ),
            )
        }
    }

    private suspend fun resolveSourceLanguage(request: TranslationRequest): TranslationLanguageTag? {
        return when (val selection = request.sourceLanguage) {
            is TranslationSourceLanguageSelection.Explicit -> selection.language
            TranslationSourceLanguageSelection.Automatic -> sourceLanguageDetectors.firstNotNullOfOrNull { detector ->
                when (val result = detector.detect(request.text)) {
                    is TranslationSourceLanguageDetection.Detected -> result.language
                    TranslationSourceLanguageDetection.Undetermined,
                    is TranslationSourceLanguageDetection.Unavailable,
                    -> null
                }
            }
        }
    }

    private fun resolveEngine(selection: TranslationEngineSelection): TranslationEngine? {
        return when (selection) {
            TranslationEngineSelection.Automatic -> engineRegistry.engines.singleOrNull()
            is TranslationEngineSelection.Explicit -> engineRegistry.find(selection.engine)
        }
    }

    private fun missingEngine(selection: TranslationEngineSelection): TranslationPreparation {
        val known = knownEngineCatalog.knownEngines
        return when (selection) {
            TranslationEngineSelection.Automatic -> {
                if (engineRegistry.engines.isEmpty()) {
                    TranslationPreparation.Unavailable(TranslationUnavailableReason.NoEngineAvailable)
                } else {
                    TranslationPreparation.EngineChoiceRequired(
                        reason = TranslationEngineChoiceReason.NoSelection,
                        engines = known,
                    )
                }
            }

            is TranslationEngineSelection.Explicit -> TranslationPreparation.EngineChoiceRequired(
                reason = TranslationEngineChoiceReason.SelectedEngineUnavailable(selection.engine),
                engines = known,
            )
        }
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
