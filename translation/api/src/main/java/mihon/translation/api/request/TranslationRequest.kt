package mihon.translation.api.request

import mihon.language.api.identification.TextLanguageResolutionContext
import mihon.language.api.tag.LanguageTag
import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.api.engine.TranslationEngineSelection

data class TranslationRequest(
    val text: String,
    val sourceLanguage: TranslationSourceLanguageSelection = TranslationSourceLanguageSelection.Automatic,
    val targetLanguage: TranslationTargetLanguageSelection = TranslationTargetLanguageSelection.Default,
    val engine: TranslationEngineSelection = TranslationEngineSelection.ProfileDefault,
    val languageContext: TextLanguageResolutionContext = TextLanguageResolutionContext(),
)

sealed interface TranslationSourceLanguageSelection {
    data object Automatic : TranslationSourceLanguageSelection

    data class Explicit(
        val language: LanguageTag,
    ) : TranslationSourceLanguageSelection
}

sealed interface TranslationTargetLanguageSelection {
    data object Default : TranslationTargetLanguageSelection

    data class Explicit(
        val language: LanguageTag,
    ) : TranslationTargetLanguageSelection
}

data class ResolvedTranslationRequest(
    val text: String,
    val sourceLanguage: LanguageTag,
    val targetLanguage: LanguageTag,
    val engine: TranslationEngineId,
)
