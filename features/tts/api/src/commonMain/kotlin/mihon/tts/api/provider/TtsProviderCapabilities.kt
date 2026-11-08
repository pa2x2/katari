package mihon.tts.api.provider

data class TtsProviderCapabilities(
    val rangeProgress: TtsOptionalCapability,
    val speechRate: TtsParameterSupport,
    val pitch: TtsParameterSupport,
    val inputLimit: TtsInputLimit,
)

enum class TtsOptionalCapability {
    Supported,
    Unsupported,
}

sealed interface TtsParameterSupport {
    data object Unsupported : TtsParameterSupport

    data class Supported(
        val range: TtsParameterRange,
    ) : TtsParameterSupport
}

data class TtsParameterRange(
    val minimum: Float,
    val maximum: Float,
    val default: Float,
) {
    init {
        require(minimum > 0f)
        require(maximum >= minimum)
        require(default in minimum..maximum)
    }
}

sealed interface TtsInputLimit {
    data class MaximumCodePoints(
        val value: Int,
    ) : TtsInputLimit {
        init {
            require(value > 0)
        }
    }

    data object Unspecified : TtsInputLimit
}

enum class TtsVoiceProcessing {
    OnDevice,
    NetworkRequired,
    Unknown,
}
