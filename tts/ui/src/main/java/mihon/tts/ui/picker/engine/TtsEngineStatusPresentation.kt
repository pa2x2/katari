package mihon.tts.ui.picker.engine

import mihon.tts.api.engine.TtsEngineAction
import mihon.tts.api.engine.TtsEngineId
import mihon.tts.api.engine.TtsEngineState
import mihon.tts.api.engine.TtsEngineStatus

internal enum class TtsEngineStatusLabel {
    Checking,
    Ready,
    NotInstalled,
    SetupRequired,
    VoiceDataRequired,
    Unavailable,
}

internal enum class TtsEngineStatusTone {
    Ready,
    Checking,
    ActionRequired,
    Unavailable,
}

internal data class TtsEngineStatusPresentation(
    val label: TtsEngineStatusLabel,
    val tone: TtsEngineStatusTone,
    val explanation: String?,
)

internal data class TtsEngineCardModel(
    val state: TtsEngineState,
    val selected: Boolean,
    val selectable: Boolean,
    val status: TtsEngineStatusPresentation,
    val action: TtsEngineAction?,
    val canManage: Boolean,
)

internal fun projectTtsEngineCard(
    state: TtsEngineState,
    selectedEngine: TtsEngineId?,
    canManage: Boolean,
): TtsEngineCardModel {
    return TtsEngineCardModel(
        state = state,
        selected = state.engine.id == selectedEngine,
        selectable = state.status == TtsEngineStatus.Ready,
        status = state.status.toPresentation(),
        action = state.action,
        canManage = canManage,
    )
}

private fun TtsEngineStatus.toPresentation(): TtsEngineStatusPresentation {
    return when (this) {
        TtsEngineStatus.Checking -> TtsEngineStatusPresentation(
            TtsEngineStatusLabel.Checking,
            TtsEngineStatusTone.Checking,
            null,
        )
        TtsEngineStatus.Ready -> TtsEngineStatusPresentation(
            TtsEngineStatusLabel.Ready,
            TtsEngineStatusTone.Ready,
            null,
        )
        TtsEngineStatus.NotInstalled -> TtsEngineStatusPresentation(
            TtsEngineStatusLabel.NotInstalled,
            TtsEngineStatusTone.ActionRequired,
            null,
        )
        is TtsEngineStatus.ConfigurationRequired -> TtsEngineStatusPresentation(
            TtsEngineStatusLabel.SetupRequired,
            TtsEngineStatusTone.ActionRequired,
            reason,
        )
        is TtsEngineStatus.ProviderDisclosureRequired -> TtsEngineStatusPresentation(
            TtsEngineStatusLabel.SetupRequired,
            TtsEngineStatusTone.ActionRequired,
            disclosure.message,
        )
        is TtsEngineStatus.VoiceDataRequired -> TtsEngineStatusPresentation(
            TtsEngineStatusLabel.VoiceDataRequired,
            TtsEngineStatusTone.ActionRequired,
            reason,
        )
        is TtsEngineStatus.Unavailable -> TtsEngineStatusPresentation(
            TtsEngineStatusLabel.Unavailable,
            TtsEngineStatusTone.Unavailable,
            reason,
        )
        is TtsEngineStatus.Failed -> TtsEngineStatusPresentation(
            TtsEngineStatusLabel.Unavailable,
            TtsEngineStatusTone.Unavailable,
            reason,
        )
    }
}
