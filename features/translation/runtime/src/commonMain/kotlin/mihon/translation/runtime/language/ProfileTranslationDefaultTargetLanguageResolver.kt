package mihon.translation.runtime.language

import mihon.language.api.tag.LanguageTag
import mihon.translation.api.language.TranslationDefaultTarget
import mihon.translation.api.request.TranslationTargetLanguageSelection
import mihon.translation.runtime.feature.TranslationDefaultTargetLanguageResolver
import mihon.translation.runtime.preference.ProfileTranslationPreferences
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
