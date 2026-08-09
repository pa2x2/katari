package mihon.tts.api.playback

import kotlinx.coroutines.flow.StateFlow
import mihon.tts.api.engine.TtsEngineId
import mihon.tts.api.preparation.TtsPreparation

/**
 * One generation-bound playback handle.
 *
 * [stop] can affect only this handle's generation, so a stale surface cannot stop newer playback.
 */
interface TtsPlaybackSession {
    val state: StateFlow<TtsPlaybackState>

    suspend fun stop(): TtsStopResult
}

sealed interface TtsPlaybackState {
    data object Starting : TtsPlaybackState

    data class Speaking(
        val range: TtsTextRange? = null,
    ) : TtsPlaybackState

    data object Completed : TtsPlaybackState

    data class Stopped(
        val reason: TtsStopReason,
    ) : TtsPlaybackState

    data class Failed(
        val reason: TtsPlaybackFailureReason,
    ) : TtsPlaybackState
}

data class TtsTextRange(
    val startInclusive: Int,
    val endExclusive: Int,
) {
    init {
        require(startInclusive >= 0)
        require(endExclusive > startInclusive)
    }
}

enum class TtsStopReason {
    Requested,
    Replaced,
    AudioFocusLost,
    OwnerDisposed,
}

sealed interface TtsStopResult {
    data object Stopped : TtsStopResult

    data object AlreadyTerminal : TtsStopResult

    data object Superseded : TtsStopResult

    data class Failed(
        val reason: String,
    ) : TtsStopResult {
        init {
            require(reason.isNotBlank())
        }
    }
}

sealed interface TtsPlaybackStart {
    data class Started(
        val session: TtsPlaybackSession,
    ) : TtsPlaybackStart

    data class PreparationChanged(
        val preparation: TtsPreparation,
    ) : TtsPlaybackStart

    data class Failed(
        val reason: TtsPlaybackFailureReason,
    ) : TtsPlaybackStart
}

sealed interface TtsPlaybackFailureReason {
    data object InvalidReadyTts : TtsPlaybackFailureReason

    data class ProviderFailure(
        val engine: TtsEngineId,
        val message: String? = null,
    ) : TtsPlaybackFailureReason {
        init {
            require(message == null || message.isNotBlank())
        }
    }
}
