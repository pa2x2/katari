package mihon.tts.api.voice

import mihon.tts.api.engine.TtsEngineId

sealed interface TtsVoiceInspection {
    data class Available(
        val engine: TtsEngineId,
        val voices: List<TtsVoice>,
    ) : TtsVoiceInspection

    data class VoiceDataRequired(
        val engine: TtsEngineId,
        val reason: String? = null,
    ) : TtsVoiceInspection {
        init {
            require(reason == null || reason.isNotBlank())
        }
    }

    data class Unavailable(
        val engine: TtsEngineId,
        val reason: String? = null,
    ) : TtsVoiceInspection {
        init {
            require(reason == null || reason.isNotBlank())
        }
    }

    data class Failed(
        val engine: TtsEngineId,
        val reason: String,
    ) : TtsVoiceInspection {
        init {
            require(reason.isNotBlank())
        }
    }
}
