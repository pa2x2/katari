package mihon.translation.api

interface TranslationFeature {
    suspend fun prepare(request: TranslationRequest): TranslationPreparation

    suspend fun translate(ready: ReadyTranslation): TranslationExecution
}
