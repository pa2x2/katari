package mihon.tts.ui.settings

import mihon.language.api.tag.LanguageTag
import mihon.tts.api.engine.TtsEngineId
import mihon.tts.api.engine.TtsEngineInspection
import mihon.tts.api.engine.TtsEngineState
import mihon.tts.api.playback.TtsPlaybackFailureReason
import mihon.tts.api.preparation.TtsPreparation
import mihon.tts.api.voice.TtsDefaultVoiceSelection
import mihon.tts.api.voice.TtsVoice
import mihon.tts.api.voice.TtsVoiceId

data class TtsSettingsState(
    val engineInspection: TtsEngineInspection,
    val voiceCatalog: TtsVoiceCatalogState,
    val voiceOverrides: Map<LanguageTag, TtsVoiceId>,
    val speechRate: Float,
    val pitch: Float,
    val allowNetworkVoices: Boolean,
    val previewLanguage: LanguageTag,
    val preview: TtsPreviewState = TtsPreviewState.Idle,
    val defaultVoice: TtsDefaultVoiceSelection = TtsDefaultVoiceSelection.EngineDefault,
    val hasUnsavedProfileChanges: Boolean = false,
    val previewVoice: TtsVoiceId? = null,
) {
    val selectedEngine: TtsEngineId?
        get() = engineInspection.selectedEngine

    val selectedEngineState: TtsEngineState?
        get() = engineInspection.engines.singleOrNull { it.engine.id == selectedEngine }

    val resolvedDefaultVoice: TtsVoice?
        get() {
            val catalog = voiceCatalog as? TtsVoiceCatalogState.Available ?: return null
            val voiceId = when (val selection = defaultVoice) {
                TtsDefaultVoiceSelection.EngineDefault -> catalog.defaultVoice
                is TtsDefaultVoiceSelection.Explicit -> selection.voice
            }
            return catalog.voices.singleOrNull { it.id == voiceId }
        }
}

sealed interface TtsVoiceCatalogState {
    data object NoEngine : TtsVoiceCatalogState

    data class Loading(
        val engine: TtsEngineId,
    ) : TtsVoiceCatalogState

    data class Available(
        val engine: TtsEngineId,
        val voices: List<TtsVoice>,
        val defaultVoice: TtsVoiceId? = null,
    ) : TtsVoiceCatalogState

    data class VoiceDataRequired(
        val engine: TtsEngineId,
        val reason: String?,
    ) : TtsVoiceCatalogState

    data class Unavailable(
        val engine: TtsEngineId,
        val reason: String?,
    ) : TtsVoiceCatalogState

    data class Failed(
        val engine: TtsEngineId,
        val reason: String,
    ) : TtsVoiceCatalogState
}

sealed interface TtsPreviewState {
    data object Idle : TtsPreviewState

    data object Preparing : TtsPreviewState

    data object Speaking : TtsPreviewState

    data class ActionRequired(
        val preparation: TtsPreparation,
    ) : TtsPreviewState

    data class Failed(
        val reason: TtsPlaybackFailureReason?,
        val message: String? = null,
    ) : TtsPreviewState
}
