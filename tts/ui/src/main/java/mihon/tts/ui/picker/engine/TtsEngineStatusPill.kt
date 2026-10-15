package mihon.tts.ui.picker.engine

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import mihon.presentation.kit.surface.StatusPill
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun TtsEngineStatusPill(status: TtsEngineStatusPresentation) {
    val label = stringResource(
        when (status.label) {
            TtsEngineStatusLabel.Checking -> MR.strings.tts_settings_engine_status_checking
            TtsEngineStatusLabel.Ready -> MR.strings.tts_settings_engine_status_ready
            TtsEngineStatusLabel.NotInstalled -> MR.strings.tts_settings_engine_status_not_installed
            TtsEngineStatusLabel.SetupRequired -> MR.strings.tts_settings_engine_status_setup_required
            TtsEngineStatusLabel.VoiceDataRequired -> MR.strings.tts_settings_engine_status_voice_data_required
            TtsEngineStatusLabel.Unavailable -> MR.strings.tts_settings_engine_status_unavailable
        },
    )
    StatusPill(
        text = label,
        tone = status.tone,
        modifier = Modifier.semantics { stateDescription = label },
    )
}
