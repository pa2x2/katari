package mihon.translation.api

import mihon.translation.api.preparation.ReadyTranslation
import mihon.translation.api.preparation.TranslationPreparation
import mihon.translation.api.preparation.TranslationRoutePreparation
import mihon.translation.api.request.TranslationRequest
import mihon.translation.api.request.TranslationRouteRequest
import mihon.translation.api.result.TranslationExecution

interface TranslationFeature {
    suspend fun prepare(request: TranslationRequest): TranslationPreparation

    /** Checks everything [prepare] would for text in the route's source language, without any text. */
    suspend fun prepareRoute(route: TranslationRouteRequest): TranslationRoutePreparation

    suspend fun translate(ready: ReadyTranslation): TranslationExecution
}
