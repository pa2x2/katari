package mihon.translation.ui.presentation.content

import androidx.compose.runtime.Composable
import mihon.translation.api.preparation.TranslationRejectionReason
import mihon.translation.api.preparation.TranslationSystemSetupReason
import mihon.translation.api.preparation.TranslationUnavailableReason
import mihon.translation.ui.picker.language.displayName
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun TranslationSystemSetupReason.message(): String {
    return when (this) {
        TranslationSystemSetupReason.ServiceDisabled ->
            stringResource(MR.strings.translation_service_disabled)
        TranslationSystemSetupReason.LanguageModelsRequired ->
            stringResource(MR.strings.translation_system_setup_required)
        is TranslationSystemSetupReason.ProviderActionRequired ->
            stringResource(MR.strings.translation_provider_action_required, description)
    }
}

@Composable
internal fun TranslationUnavailableReason.message(): String {
    return when (this) {
        is TranslationUnavailableReason.UnsupportedOs ->
            stringResource(MR.strings.translation_unsupported_os, minimumApi)
        TranslationUnavailableReason.ServiceMissing ->
            stringResource(MR.strings.translation_service_missing)
        TranslationUnavailableReason.SystemSettingsUnavailable ->
            stringResource(MR.strings.translation_settings_unavailable)
        is TranslationUnavailableReason.UnsupportedLanguage ->
            stringResource(MR.strings.translation_unsupported_language, language.displayName())
        is TranslationUnavailableReason.UnsupportedLanguagePair ->
            stringResource(
                MR.strings.translation_unsupported_pair,
                source.displayName(),
                target.displayName(),
            )
        is TranslationUnavailableReason.EngineUnavailable ->
            stringResource(MR.strings.translation_engine_unavailable, engine.value, reason)
    }
}

@Composable
internal fun TranslationRejectionReason.message(): String {
    return when (this) {
        TranslationRejectionReason.BlankInput -> stringResource(MR.strings.translation_blank_input)
        is TranslationRejectionReason.InputTooLarge -> stringResource(
            MR.strings.translation_input_too_large,
            actualCodePoints,
            maximumCodePoints,
        )
    }
}
