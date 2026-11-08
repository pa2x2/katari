package app.katari.tts.ui.engine

import app.katari.tts.api.engine.TtsEngineStatus
import app.katari.tts.api.playback.TtsPlaybackFailureReason
import app.katari.tts.api.preparation.TtsPreparation
import app.katari.tts.api.preparation.TtsUnavailableReason

/** Why the engine says it isn't ready, in the words it writes for the user; null when it said nothing. */
fun TtsEngineStatus.engineExplanation(): String? = when (this) {
    TtsEngineStatus.Checking,
    TtsEngineStatus.Ready,
    -> null
    is TtsEngineStatus.NotInstalled -> reason
    is TtsEngineStatus.ConfigurationRequired -> reason
    is TtsEngineStatus.ProviderDisclosureRequired -> disclosure.message
    is TtsEngineStatus.VoiceDataRequired -> reason
    is TtsEngineStatus.Unavailable -> reason
    is TtsEngineStatus.Failed -> reason
}

/** Why the engine says speech can't run, in the words it writes for the user; null when it said nothing. */
fun TtsPreparation.engineExplanation(): String? {
    val unavailable = this as? TtsPreparation.Unavailable ?: return null
    return when (val reason = unavailable.reason) {
        is TtsUnavailableReason.EngineUnavailable -> reason.reason
        is TtsUnavailableReason.ProviderInspectionFailed -> reason.reason
        TtsUnavailableReason.ServiceMissing,
        is TtsUnavailableReason.UnsupportedLanguage,
        is TtsUnavailableReason.NetworkVoiceProhibited,
        -> null
    }
}

/** Why the engine says speech stopped, in the words it writes for the user; null when it said nothing. */
fun TtsPlaybackFailureReason.engineExplanation(): String? = (this as? TtsPlaybackFailureReason.ProviderFailure)?.message
