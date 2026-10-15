package eu.kanade.presentation.more.settings.screen.tts.presentation

import androidx.compose.runtime.Composable
import mihon.tts.api.provider.TtsVoiceProcessing
import mihon.tts.api.voice.TtsDefaultVoiceSelection
import mihon.tts.ui.settings.TtsSettingsState
import mihon.tts.ui.settings.displayName
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun engineSummary(state: TtsSettingsState): String {
    if (!state.engineInspection.selectionResolved) {
        return stringResource(MR.strings.tts_settings_engine_status_checking)
    }
    if (state.selectedEngine == null) {
        return stringResource(MR.strings.tts_settings_choose_engine)
    }
    return state.selectedEngineState?.engine?.engineName
        ?: stringResource(MR.strings.tts_settings_engine_unavailable)
}

@Composable
internal fun defaultVoiceSummary(state: TtsSettingsState): String {
    val voice = state.resolvedDefaultVoice
    if (voice == null) return stringResource(MR.strings.tts_settings_voice_unavailable)
    return when (state.defaultVoice) {
        TtsDefaultVoiceSelection.EngineDefault -> stringResource(
            MR.strings.tts_settings_voice_engine_default_value,
            voice.language.displayName(),
            voice.processing.label(),
        )
        is TtsDefaultVoiceSelection.Explicit -> stringResource(
            MR.strings.tts_settings_voice_value,
            voice.name,
            voice.language.displayName(),
            voice.processing.label(),
        )
    }
}

@Composable
internal fun voiceOverridesSummary(state: TtsSettingsState): String {
    return if (state.voiceOverrides.isEmpty()) {
        stringResource(MR.strings.tts_settings_no_voice_overrides)
    } else {
        stringResource(MR.strings.tts_settings_voice_overrides_count, state.voiceOverrides.size)
    }
}

@Composable
private fun TtsVoiceProcessing.label(): String {
    return stringResource(
        when (this) {
            TtsVoiceProcessing.OnDevice -> MR.strings.tts_settings_voice_on_device
            TtsVoiceProcessing.NetworkRequired -> MR.strings.tts_settings_voice_network
            TtsVoiceProcessing.Unknown -> MR.strings.tts_settings_voice_unknown
        },
    )
}
