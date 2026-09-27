package mihon.text.recognition.api.pipeline

import mihon.language.api.tag.LanguageTag
import mihon.text.recognition.api.component.TextRecognitionComponentId
import mihon.text.recognition.api.provider.TextRecognitionProviderId

/** How text is located and read. */
sealed interface TextRecognitionPipeline {
    val components: List<TextRecognitionComponentId>

    /** A detector finds regions; a recognizer reads each of them. */
    data class Staged(
        val detector: TextRecognitionComponentId,
        val recognizer: TextRecognitionComponentId,
    ) : TextRecognitionPipeline {
        override val components: List<TextRecognitionComponentId>
            get() = listOf(detector, recognizer)
    }

    /** One engine finds and reads text. */
    data class Engine(
        val engine: TextRecognitionComponentId,
    ) : TextRecognitionPipeline {
        override val components: List<TextRecognitionComponentId>
            get() = listOf(engine)
    }
}

@JvmInline
value class TextRecognitionPresetId(
    val value: String,
) {
    init {
        require(ID_PATTERN.matches(value)) { "Invalid text recognition preset id: '$value'" }
    }

    private companion object {
        val ID_PATTERN = Regex("""[a-z][a-z0-9]*(?:[.-][a-z0-9]+)*""")
    }
}

/** A named pipeline [provider] recommends for the listed languages. */
data class TextRecognitionPreset(
    val id: TextRecognitionPresetId,
    val provider: TextRecognitionProviderId,
    val displayName: String,
    val description: String,
    val languages: Set<LanguageTag>,
    val pipeline: TextRecognitionPipeline,
) {
    init {
        require(displayName.isNotBlank())
        require(description.isNotBlank())
        require(languages.isNotEmpty())
    }
}

/** A profile's pipeline choice for one language: a preset, or an explicit override. */
sealed interface TextRecognitionPipelineSelection {
    data class Preset(
        val preset: TextRecognitionPresetId,
    ) : TextRecognitionPipelineSelection

    data class Custom(
        val pipeline: TextRecognitionPipeline,
    ) : TextRecognitionPipelineSelection
}
