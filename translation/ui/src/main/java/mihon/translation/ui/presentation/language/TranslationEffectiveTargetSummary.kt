package mihon.translation.ui.presentation.language

import androidx.compose.runtime.Composable
import mihon.translation.ui.picker.language.displayName
import mihon.translation.ui.session.language.TranslationEffectiveTarget
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The target language with where it comes from: the series, the app language, or the profile default. */
@Composable
fun translationEffectiveTargetSummary(target: TranslationEffectiveTarget): String {
    val language = target.language.displayName()
    return when (target) {
        is TranslationEffectiveTarget.Kept -> stringResource(MR.strings.translation_language_this_series, language)
        is TranslationEffectiveTarget.Default -> stringResource(
            if (target.target.followsAppLanguage) {
                MR.strings.translation_language_app_language
            } else {
                MR.strings.translation_language_profile_default
            },
            language,
        )
    }
}
