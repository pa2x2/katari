package mihon.translation.api

import kotlinx.coroutines.flow.Flow
import mihon.translation.api.preparation.ReadyTranslation
import mihon.translation.api.preparation.TranslationPreparation
import mihon.translation.api.preparation.TranslationRoutePreparation
import mihon.translation.api.request.TranslationBatch
import mihon.translation.api.request.TranslationRequest
import mihon.translation.api.request.TranslationRouteRequest
import mihon.translation.api.result.TranslationBatchUpdate
import mihon.translation.api.result.TranslationExecution

interface TranslationFeature {
    suspend fun prepare(request: TranslationRequest): TranslationPreparation

    /** Checks everything [prepare] would for text in the route's source language, without any text. */
    suspend fun prepareRoute(route: TranslationRouteRequest): TranslationRoutePreparation

    suspend fun translate(ready: ReadyTranslation): TranslationExecution

    /**
     * Translates texts that belong together without asking the user anything, reporting each as soon as its
     * translation is known. Only engines that answer inline translate this way; with any other, every text fails.
     */
    fun translateBatch(batch: TranslationBatch): Flow<TranslationBatchUpdate>
}
