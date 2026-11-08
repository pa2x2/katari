package mihon.translation.ui.picker.language

import androidx.compose.runtime.Composable
import mihon.language.api.tag.LanguageTag
import mihon.translation.api.language.TranslationDefaultTarget
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The target row that follows the profile's default, named after the app language when the default follows it. */
@Composable
fun translationDefaultTargetOption(
    target: TranslationDefaultTarget,
    selected: Boolean,
): TranslationLanguageDefaultOption = TranslationLanguageDefaultOption(
    label = stringResource(
        if (target.followsAppLanguage) {
            MR.strings.translation_settings_target_app_language
        } else {
            MR.strings.translator_profile_default
        },
    ),
    supporting = target.language.displayName(),
    selected = selected,
)

/** The source row that detects the language, showing what was [detected] and what the text [declared]. */
@Composable
fun translationAutomaticSourceOption(
    detected: LanguageTag?,
    declared: LanguageTag?,
    selected: Boolean,
): TranslationLanguageDefaultOption = TranslationLanguageDefaultOption(
    label = stringResource(MR.strings.translation_source_automatic),
    supporting = when {
        detected != null && declared != null -> stringResource(
            MR.strings.translation_source_automatic_detected_declared,
            detected.displayName(),
            declared.displayName(),
        )
        detected != null ->
            stringResource(MR.strings.translation_source_automatic_detected, detected.displayName())
        declared != null ->
            stringResource(MR.strings.translation_source_automatic_declared, declared.displayName())
        else -> stringResource(MR.strings.translation_source_automatic_summary)
    },
    selected = selected,
)
