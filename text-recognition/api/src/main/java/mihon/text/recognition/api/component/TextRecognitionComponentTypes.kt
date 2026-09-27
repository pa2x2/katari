package mihon.text.recognition.api.component

import mihon.language.api.tag.LanguageTag

@JvmInline
value class TextRecognitionComponentId(
    val value: String,
) {
    init {
        require(ID_PATTERN.matches(value)) { "Invalid text recognition component id: '$value'" }
    }

    private companion object {
        val ID_PATTERN = Regex("""[a-z][a-z0-9]*(?:[.-][a-z0-9]+)*""")
    }
}

/** The pipeline stage a component implements. */
enum class TextRecognitionComponentRole {
    /** Locates text regions in an image. */
    Detector,

    /** Reads the text of one cropped region. */
    Recognizer,

    /** Locates and reads text in one step. */
    Engine,
}

/**
 * Catalog description of a component, present even when the component is excluded from the current build.
 *
 * @property languages languages the component can read; empty for language-independent detectors.
 */
data class KnownTextRecognitionComponent(
    val id: TextRecognitionComponentId,
    val role: TextRecognitionComponentRole,
    val providerName: String,
    val displayName: String,
    val description: String,
    val languages: Set<LanguageTag>,
    val buildAvailability: TextRecognitionBuildAvailability,
    val documentationUrl: String? = null,
) {
    init {
        require(providerName.isNotBlank())
        require(displayName.isNotBlank())
        require(description.isNotBlank())
        require(role == TextRecognitionComponentRole.Detector || languages.isNotEmpty()) {
            "Text recognition component ${id.value} reads text but declares no language"
        }
        require(documentationUrl == null || documentationUrl.isNotBlank())
    }
}

sealed interface TextRecognitionBuildAvailability {
    data object Included : TextRecognitionBuildAvailability

    data class NotIncluded(
        val reason: String,
    ) : TextRecognitionBuildAvailability {
        init {
            require(reason.isNotBlank())
        }
    }
}
