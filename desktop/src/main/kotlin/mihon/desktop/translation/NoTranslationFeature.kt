package mihon.desktop.translation

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import mihon.translation.api.TranslationFeature
import mihon.translation.api.preparation.ReadyTranslation
import mihon.translation.api.preparation.TranslationEngineChoiceReason
import mihon.translation.api.preparation.TranslationPreparation
import mihon.translation.api.preparation.TranslationRoutePreparation
import mihon.translation.api.request.TranslationBatch
import mihon.translation.api.request.TranslationRequest
import mihon.translation.api.request.TranslationRouteRequest
import mihon.translation.api.result.TranslationBatchUpdate
import mihon.translation.api.result.TranslationExecution
import mihon.translation.api.result.TranslationFailureReason

/**
 * Translation with no engine to translate: every request asks for an engine and offers none. Stands in until desktop
 * has a translation engine.
 */
@Inject
class NoTranslationFeature : TranslationFeature {
    override suspend fun prepare(request: TranslationRequest): TranslationPreparation = NoEngine

    override suspend fun prepareRoute(route: TranslationRouteRequest): TranslationRoutePreparation = NoEngine

    override suspend fun translate(ready: ReadyTranslation): TranslationExecution =
        TranslationExecution.Failed(TranslationFailureReason.InvalidReadyTranslation)

    override fun translateBatch(batch: TranslationBatch): Flow<TranslationBatchUpdate> =
        flowOf(TranslationBatchUpdate.Blocked(NoEngine))

    private companion object {
        val NoEngine = TranslationPreparation.EngineChoiceRequired(
            reason = TranslationEngineChoiceReason.NoEngineConfigured,
            engines = emptyList(),
        )
    }
}
