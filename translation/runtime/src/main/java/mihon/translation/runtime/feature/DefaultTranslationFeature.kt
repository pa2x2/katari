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
import mihon.translation.spi.TranslationAutomaticSelectionPriority
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
    private val preferredEngineSelection: () -> TranslationEngineSelection = {
        TranslationEngineSelection.Automatic
    },
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

        val engineSelection = when (request.engine) {
            TranslationEngineSelection.Automatic -> preferredEngineSelection()
            is TranslationEngineSelection.Explicit -> request.engine
        }
        return when (engineSelection) {
            TranslationEngineSelection.Automatic -> prepareAutomatically(
                request = request,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                codePointCount = codePointCount,
            )

            is TranslationEngineSelection.Explicit -> prepareExplicitly(
                request = request,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                codePointCount = codePointCount,
                selection = engineSelection,
            )
        }
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

        val refreshed = prepared.engine.revalidate(prepared.engineRequest)
        if (refreshed !is TranslationEnginePreparation.Ready) {
            return TranslationExecution.PreparationChanged(
                refreshed.toApi(prepared.engine, prepared.request),
            )
        }

        return when (val execution = prepared.engine.translate(refreshed.request)) {
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

    private suspend fun prepareExplicitly(
        request: TranslationRequest,
        sourceLanguage: TranslationLanguageTag,
        targetLanguage: TranslationLanguageTag,
        codePointCount: Int,
        selection: TranslationEngineSelection.Explicit,
    ): TranslationPreparation {
        val engine = engineRegistry.find(selection.engine) ?: return missingEngine(selection)
        val maximumCodePoints = engine.effectiveMaximumInputCodePoints()
        if (codePointCount > maximumCodePoints) {
            return inputTooLarge(codePointCount, maximumCodePoints)
        }
        val resolvedRequest = request.resolve(sourceLanguage, targetLanguage, engine)
        return engine.prepare(resolvedRequest).toApi(engine, resolvedRequest)
    }

    private suspend fun prepareAutomatically(
        request: TranslationRequest,
        sourceLanguage: TranslationLanguageTag,
        targetLanguage: TranslationLanguageTag,
        codePointCount: Int,
    ): TranslationPreparation {
        if (engineRegistry.engines.isEmpty()) {
            return missingEngine(TranslationEngineSelection.Automatic)
        }
        val eligibleEngines = engineRegistry.engines.filter { engine ->
            codePointCount <= engine.effectiveMaximumInputCodePoints()
        }
        if (eligibleEngines.isEmpty()) {
            val maximumCodePoints = engineRegistry.engines.maxOf { engine ->
                engine.effectiveMaximumInputCodePoints()
            }
            return inputTooLarge(codePointCount, maximumCodePoints)
        }

        val candidates = eligibleEngines.map { engine ->
            val resolvedRequest = request.resolve(sourceLanguage, targetLanguage, engine)
            AutomaticCandidate(
                engine = engine,
                request = resolvedRequest,
                preparation = engine.prepare(resolvedRequest),
            )
        }
        val ready = candidates.filter { candidate ->
            candidate.preparation is TranslationEnginePreparation.Ready
        }
        val actionableSetup = candidates.filter { candidate ->
            candidate.preparation !is TranslationEnginePreparation.Ready &&
                candidate.preparation !is TranslationEnginePreparation.Unavailable
        }
        val selected = when {
            ready.isNotEmpty() -> ready.highestPriority { it.ready }
            actionableSetup.isNotEmpty() -> actionableSetup.highestPriority { it.setup }
            else -> candidates.highestPriority { it.setup }
        }
        return selected.preparation.toApi(selected.engine, selected.request)
    }

    private fun TranslationRequest.resolve(
        sourceLanguage: TranslationLanguageTag,
        targetLanguage: TranslationLanguageTag,
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

    private fun List<AutomaticCandidate>.highestPriority(
        priority: (TranslationAutomaticSelectionPriority) -> Int,
    ): AutomaticCandidate {
        val maximum = maxOf { candidate -> priority(candidate.engine.automaticSelectionPriority) }
        return first { candidate -> priority(candidate.engine.automaticSelectionPriority) == maximum }
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

    private data class AutomaticCandidate(
        val engine: TranslationEngine,
        val request: ResolvedTranslationRequest,
        val preparation: TranslationEnginePreparation,
    )

    private companion object {
        const val SHARED_MAXIMUM_CODE_POINTS = 10_000
    }
}
