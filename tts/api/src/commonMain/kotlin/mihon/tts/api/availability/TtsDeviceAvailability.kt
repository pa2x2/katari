package mihon.tts.api.availability

import mihon.tts.api.engine.TtsEngineId

/** Request-independent availability of profile-selected short-form speech. */
sealed interface TtsDeviceAvailability {
    data object Available : TtsDeviceAvailability

    data object EngineNotConfigured : TtsDeviceAvailability

    data object NoEnginesInstalled : TtsDeviceAvailability

    data class SelectedEngineMissing(
        val engine: TtsEngineId,
    ) : TtsDeviceAvailability

    data class SelectedEngineUnavailable(
        val engine: TtsEngineId,
        val reason: String? = null,
    ) : TtsDeviceAvailability {
        init {
            require(reason == null || reason.isNotBlank())
        }
    }

    data class ProviderFailure(
        val engine: TtsEngineId,
        val reason: String,
    ) : TtsDeviceAvailability {
        init {
            require(reason.isNotBlank())
        }
    }
}
