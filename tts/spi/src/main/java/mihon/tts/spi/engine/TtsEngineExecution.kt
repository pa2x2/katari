package mihon.tts.spi.engine

import kotlinx.coroutines.flow.Flow
import mihon.tts.api.playback.TtsTextRange

sealed interface TtsEngineExecution {
    data class Started(
        val playback: TtsEnginePlayback,
    ) : TtsEngineExecution

    data class PreparationChanged(
        val preparation: TtsEnginePreparation,
    ) : TtsEngineExecution

    data class Failed(
        val message: String? = null,
    ) : TtsEngineExecution {
        init {
            require(message == null || message.isNotBlank())
        }
    }
}

interface TtsEnginePlayback {
    val events: Flow<TtsEnginePlaybackEvent>

    suspend fun stop(): TtsEngineStopResult
}

sealed interface TtsEnginePlaybackEvent {
    data object Started : TtsEnginePlaybackEvent

    data class RangeStarted(
        val range: TtsTextRange,
    ) : TtsEnginePlaybackEvent

    data object Completed : TtsEnginePlaybackEvent

    data object Stopped : TtsEnginePlaybackEvent

    data class Failed(
        val message: String? = null,
    ) : TtsEnginePlaybackEvent {
        init {
            require(message == null || message.isNotBlank())
        }
    }
}

sealed interface TtsEngineStopResult {
    data object Stopped : TtsEngineStopResult

    data object AlreadyTerminal : TtsEngineStopResult

    data class Failed(
        val message: String? = null,
    ) : TtsEngineStopResult {
        init {
            require(message == null || message.isNotBlank())
        }
    }
}
