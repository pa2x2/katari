package mihon.translation.api

data class TranslationResult(
    val translatedText: String,
    val sourceLanguage: TranslationLanguageTag,
    val targetLanguage: TranslationLanguageTag,
    val presentation: TranslationProviderPresentation,
) {
    init {
        require(translatedText.isNotBlank())
    }
}

sealed interface TranslationExecution {
    data class Success(
        val result: TranslationResult,
    ) : TranslationExecution

    data class PreparationChanged(
        val preparation: TranslationPreparation,
    ) : TranslationExecution

    data class ProviderSurfaceOpened(
        val presentation: TranslationProviderPresentation,
    ) : TranslationExecution

    data class Failed(
        val reason: TranslationFailureReason,
    ) : TranslationExecution
}

sealed interface TranslationFailureReason {
    data object InvalidReadyTranslation : TranslationFailureReason

    data class ProviderFailure(
        val engine: TranslationEngineId,
        val message: String?,
    ) : TranslationFailureReason
}
