package mihon.translation.api.request

import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.api.engine.TranslationEngineSelection
import mihon.translation.api.language.TranslationLanguageTag

data class TranslationRequest(
    val text: String,
    val sourceLanguage: TranslationSourceLanguageSelection = TranslationSourceLanguageSelection.Automatic,
    val targetLanguage: TranslationTargetLanguageSelection = TranslationTargetLanguageSelection.Default,
    val engine: TranslationEngineSelection = TranslationEngineSelection.ProfileDefault,
)

sealed interface TranslationSourceLanguageSelection {
    data object Automatic : TranslationSourceLanguageSelection

    data class Explicit(
        val language: TranslationLanguageTag,
    ) : TranslationSourceLanguageSelection
}

sealed interface TranslationTargetLanguageSelection {
    data object Default : TranslationTargetLanguageSelection

    data class Explicit(
        val language: TranslationLanguageTag,
    ) : TranslationTargetLanguageSelection
}

data class ResolvedTranslationRequest(
    val text: String,
    val sourceLanguage: TranslationLanguageTag,
    val targetLanguage: TranslationLanguageTag,
    val engine: TranslationEngineId,
)
