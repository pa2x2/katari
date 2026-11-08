package mihon.tts.ui.picker.engine

import mihon.presentation.kit.surface.StatusTone
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

internal data class TtsEngineStatusPresentation(
    val label: TtsEngineStatusLabel,
    val tone: StatusTone,
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
            StatusTone.Checking,
            null,
        )
        TtsEngineStatus.Ready -> TtsEngineStatusPresentation(
            TtsEngineStatusLabel.Ready,
            StatusTone.Ready,
            null,
        )
        TtsEngineStatus.NotInstalled -> TtsEngineStatusPresentation(
            TtsEngineStatusLabel.NotInstalled,
            StatusTone.ActionRequired,
            null,
        )
        is TtsEngineStatus.ConfigurationRequired -> TtsEngineStatusPresentation(
            TtsEngineStatusLabel.SetupRequired,
            StatusTone.ActionRequired,
            reason,
        )
        is TtsEngineStatus.ProviderDisclosureRequired -> TtsEngineStatusPresentation(
            TtsEngineStatusLabel.SetupRequired,
            StatusTone.ActionRequired,
            disclosure.message,
        )
        is TtsEngineStatus.VoiceDataRequired -> TtsEngineStatusPresentation(
            TtsEngineStatusLabel.VoiceDataRequired,
            StatusTone.ActionRequired,
            reason,
        )
        is TtsEngineStatus.Unavailable -> TtsEngineStatusPresentation(
            TtsEngineStatusLabel.Unavailable,
            StatusTone.Unavailable,
            reason,
        )
        is TtsEngineStatus.Failed -> TtsEngineStatusPresentation(
            TtsEngineStatusLabel.Unavailable,
            StatusTone.Unavailable,
            reason,
        )
    }
}
