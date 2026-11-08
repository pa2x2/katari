package app.katari.translation.api.request

import app.katari.language.api.identification.TextLanguageResolutionContext
import app.katari.language.api.tag.LanguageTag
import app.katari.translation.api.engine.TranslationEngineId
import app.katari.translation.api.engine.TranslationEngineSelection

data class TranslationRequest(
    val text: String,
    val sourceLanguage: TranslationSourceLanguageSelection = TranslationSourceLanguageSelection.Automatic,
    val targetLanguage: TranslationTargetLanguageSelection = TranslationTargetLanguageSelection.Default,
    val engine: TranslationEngineSelection = TranslationEngineSelection.ProfileDefault,
    val languageContext: TextLanguageResolutionContext = TextLanguageResolutionContext(),
    val context: TranslationContext = TranslationContext.None,
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
) {
    val route: ResolvedTranslationRoute
        get() = ResolvedTranslationRoute(sourceLanguage, targetLanguage, engine)
}
