package app.katari.translation.runtime.language

import app.katari.language.api.tag.LanguageTag
import app.katari.translation.api.language.TranslationDefaultTarget
import app.katari.translation.api.request.TranslationTargetLanguageSelection
import app.katari.translation.runtime.feature.TranslationDefaultTargetLanguageResolver
import app.katari.translation.runtime.preference.ProfileTranslationPreferences
import java.util.Locale

internal class ProfileTranslationDefaultTargetLanguageResolver(
    private val preferences: ProfileTranslationPreferences,
    private val effectiveUiLocale: () -> Locale?,
) : TranslationDefaultTargetLanguageResolver {
    override fun resolve(): TranslationDefaultTarget? {
        return when (val selection = preferences.targetLanguage.get()) {
            TranslationTargetLanguageSelection.Default ->
                effectiveUiLocale()
                    ?.toLanguageTag()
                    ?.let(LanguageTag::parse)
                    ?.let { TranslationDefaultTarget(it, followsAppLanguage = true) }

            is TranslationTargetLanguageSelection.Explicit ->
                TranslationDefaultTarget(selection.language, followsAppLanguage = false)
        }
    }
}
