package app.katari.tts.ui.picker.engine

import app.katari.presentation.kit.surface.StatusTone
import app.katari.tts.api.engine.TtsEngineAction
import app.katari.tts.api.engine.TtsEngineId
import app.katari.tts.api.engine.TtsEngineState
import app.katari.tts.api.engine.TtsEngineStatus
import app.katari.tts.ui.engine.engineExplanation

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
    val (label, tone) = when (this) {
        TtsEngineStatus.Checking -> TtsEngineStatusLabel.Checking to StatusTone.Checking
        TtsEngineStatus.Ready -> TtsEngineStatusLabel.Ready to StatusTone.Ready
        is TtsEngineStatus.NotInstalled -> TtsEngineStatusLabel.NotInstalled to StatusTone.ActionRequired
        is TtsEngineStatus.ConfigurationRequired,
        is TtsEngineStatus.ProviderDisclosureRequired,
        -> TtsEngineStatusLabel.SetupRequired to StatusTone.ActionRequired
        is TtsEngineStatus.VoiceDataRequired -> TtsEngineStatusLabel.VoiceDataRequired to StatusTone.ActionRequired
        is TtsEngineStatus.Unavailable,
        is TtsEngineStatus.Failed,
        -> TtsEngineStatusLabel.Unavailable to StatusTone.Unavailable
    }
    return TtsEngineStatusPresentation(label, tone, engineExplanation())
}
