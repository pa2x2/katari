package mihon.translation.ui.picker.engine

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import mihon.presentation.kit.surface.StatusPill
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun TranslationEngineStatusPill(
    presentation: TranslationEngineStatusPresentation,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(
        when (presentation.label) {
            TranslationEngineStatusLabel.Checking -> MR.strings.translation_engine_status_checking
            TranslationEngineStatusLabel.Ready -> MR.strings.translation_engine_status_ready
            TranslationEngineStatusLabel.NotInstalled -> MR.strings.translation_engine_status_not_installed
            TranslationEngineStatusLabel.ConfigurationRequired ->
                MR.strings.translation_engine_status_setup_required
            TranslationEngineStatusLabel.ProviderDisclosureRequired ->
                MR.strings.translation_engine_status_consent_required
            TranslationEngineStatusLabel.ModelDownloadRequired ->
                MR.strings.translation_engine_status_language_data_required
            TranslationEngineStatusLabel.SystemSetupRequired ->
                MR.strings.translation_engine_status_system_setup_required
            TranslationEngineStatusLabel.SetupInProgress ->
                MR.strings.translation_engine_status_setting_up
            TranslationEngineStatusLabel.Unavailable -> MR.strings.translation_engine_status_unavailable
        },
    )
    StatusPill(
        text = label,
        tone = presentation.tone,
        modifier = modifier.semantics { stateDescription = label },
    )
}
