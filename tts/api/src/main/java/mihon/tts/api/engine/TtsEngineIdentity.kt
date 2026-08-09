package mihon.tts.api.engine

@JvmInline
value class TtsEngineId(
    val value: String,
) {
    init {
        require(ID_PATTERN.matches(value)) { "Invalid TTS engine id: '$value'" }
    }

    private companion object {
        val ID_PATTERN = Regex("""[a-z][a-z0-9]*(?:[.-][a-z0-9]+)*""")
    }
}

@JvmInline
value class TtsProviderId(
    val value: String,
) {
    init {
        require(ID_PATTERN.matches(value)) { "Invalid TTS provider id: '$value'" }
    }

    private companion object {
        val ID_PATTERN = Regex("""[a-z][a-z0-9]*(?:[.-][a-z0-9]+)*""")
    }
}

sealed interface TtsEngineSelection {
    data object ProfileDefault : TtsEngineSelection

    data class Explicit(
        val engine: TtsEngineId,
    ) : TtsEngineSelection
}
