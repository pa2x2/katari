package mihon.translation.api

import mihon.translation.api.preparation.ReadyTranslation
import mihon.translation.api.preparation.TranslationPreparation
import mihon.translation.api.request.TranslationRequest
import mihon.translation.api.result.TranslationExecution

interface TranslationFeature {
    suspend fun prepare(request: TranslationRequest): TranslationPreparation

    suspend fun translate(ready: ReadyTranslation): TranslationExecution
}
