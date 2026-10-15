package mihon.translation.ui.picker.engine

import androidx.compose.runtime.Composable
import mihon.translation.api.preparation.TranslationSystemSetupReason
import mihon.translation.api.preparation.TranslationUnavailableReason
import mihon.translation.ui.picker.language.displayName
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun TranslationEngineStatusExplanation.text(): String {
    return when (this) {
        TranslationEngineStatusExplanation.InstallProvider ->
            stringResource(MR.strings.translation_engine_install_explanation)
        TranslationEngineStatusExplanation.ProviderDisclosure ->
            stringResource(MR.strings.translation_engine_consent_explanation)
        TranslationEngineStatusExplanation.LanguageData ->
            stringResource(MR.strings.translation_engine_model_explanation)
        is TranslationEngineStatusExplanation.ProviderText -> text
        is TranslationEngineStatusExplanation.SystemSetup -> reason.text()
        is TranslationEngineStatusExplanation.Unavailable -> {
            buildReason ?: reason.text()
        }
    }
}

@Composable
private fun TranslationSystemSetupReason.text(): String {
    return when (this) {
        TranslationSystemSetupReason.ServiceDisabled ->
            stringResource(MR.strings.translation_service_disabled)
        TranslationSystemSetupReason.LanguageModelsRequired ->
            stringResource(MR.strings.translation_models_required)
        is TranslationSystemSetupReason.ProviderActionRequired -> description
    }
}

@Composable
private fun TranslationUnavailableReason.text(): String {
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
            reason
    }
}
