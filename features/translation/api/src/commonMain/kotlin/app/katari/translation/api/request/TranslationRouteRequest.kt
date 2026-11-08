package app.katari.translation.api.request

import app.katari.language.api.tag.LanguageTag
import app.katari.translation.api.engine.TranslationEngineId
import app.katari.translation.api.engine.TranslationEngineSelection

/**
 * What a [TranslationRequest] decides before any text exists: the target and engine for text known to be in
 * [sourceLanguage].
 */
data class TranslationRouteRequest(
    val sourceLanguage: LanguageTag,
    val targetLanguage: TranslationTargetLanguageSelection = TranslationTargetLanguageSelection.Default,
    val engine: TranslationEngineSelection = TranslationEngineSelection.ProfileDefault,
)

/** A route with every choice made; requests naming exactly these languages and engine translate the same way. */
data class ResolvedTranslationRoute(
    val sourceLanguage: LanguageTag,
    val targetLanguage: LanguageTag,
    val engine: TranslationEngineId,
)
