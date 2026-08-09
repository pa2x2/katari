package mihon.translation.runtime.language

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import mihon.language.api.tag.LanguageTag
import mihon.translation.api.request.TranslationTargetLanguageSelection
import mihon.translation.runtime.feature.TranslationDefaultTargetLanguageResolver
import mihon.translation.runtime.preference.ProfileTranslationPreferences
import java.util.Locale

internal class ProfileTranslationDefaultTargetLanguageResolver(
    private val preferences: ProfileTranslationPreferences,
    private val effectiveUiLocale: () -> Locale? = ::effectiveUiLocale,
) : TranslationDefaultTargetLanguageResolver {
    override fun resolve(): LanguageTag? {
        return when (val selection = preferences.targetLanguage.get()) {
            TranslationTargetLanguageSelection.Default ->
                effectiveUiLocale()?.toLanguageTag()?.let(LanguageTag::parse)

            is TranslationTargetLanguageSelection.Explicit -> selection.language
        }
    }
}

private fun effectiveUiLocale(): Locale? {
    val applicationLocales = AppCompatDelegate.getApplicationLocales()
    return applicationLocales.get(0) ?: LocaleListCompat.getAdjustedDefault().get(0)
}
