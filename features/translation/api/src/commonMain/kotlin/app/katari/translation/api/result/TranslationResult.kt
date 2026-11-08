package app.katari.translation.api.result

import app.katari.language.api.tag.LanguageTag
import app.katari.translation.api.engine.TranslationEngineId
import app.katari.translation.api.preparation.TranslationPreparation
import app.katari.translation.api.provider.TranslationProviderPresentation

data class TranslationResult(
    val translatedText: String,
    val sourceLanguage: LanguageTag,
    val targetLanguage: LanguageTag,
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
