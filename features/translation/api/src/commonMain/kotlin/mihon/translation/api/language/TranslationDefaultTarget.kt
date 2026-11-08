package mihon.translation.api.language

import mihon.language.api.tag.LanguageTag

/**
 * The target language a request uses when it does not choose one.
 *
 * [followsAppLanguage] is true when the profile has not chosen a target, so the target is Katari's app language.
 */
data class TranslationDefaultTarget(
    val language: LanguageTag,
    val followsAppLanguage: Boolean,
)
