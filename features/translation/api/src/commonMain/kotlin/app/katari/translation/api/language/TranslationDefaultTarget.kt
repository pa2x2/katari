package app.katari.translation.api.language

import app.katari.language.api.tag.LanguageTag

/**
 * The target language a request uses when it does not choose one.
 *
 * [followsAppLanguage] is true when the profile has not chosen a target, so the target is Katari's app language.
 */
data class TranslationDefaultTarget(
    val language: LanguageTag,
    val followsAppLanguage: Boolean,
)
