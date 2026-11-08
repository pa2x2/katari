package app.katari.translation.api

import app.katari.translation.api.preparation.ReadyTranslation
import app.katari.translation.api.preparation.TranslationPreparation
import app.katari.translation.api.preparation.TranslationRoutePreparation
import app.katari.translation.api.request.TranslationBatch
import app.katari.translation.api.request.TranslationRequest
import app.katari.translation.api.request.TranslationRouteRequest
import app.katari.translation.api.result.TranslationBatchUpdate
import app.katari.translation.api.result.TranslationExecution
import kotlinx.coroutines.flow.Flow

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
