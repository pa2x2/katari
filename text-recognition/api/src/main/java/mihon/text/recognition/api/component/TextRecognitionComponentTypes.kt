package mihon.text.recognition.api.component

import mihon.language.api.tag.LanguageTag
import mihon.text.recognition.api.provider.TextRecognitionProviderId

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
}

/**
 * Catalog description of a component of [provider].
 *
 * @property languages languages the component can read; empty for language-independent detectors.
 * @property scripts how [languages] share models, for summarizing them by script.
 */
data class KnownTextRecognitionComponent(
    val id: TextRecognitionComponentId,
    val provider: TextRecognitionProviderId,
    val role: TextRecognitionComponentRole,
    val displayName: String,
    val description: String,
    val languages: Set<LanguageTag>,
    val documentationUrl: String? = null,
    val scripts: List<TextRecognitionScript> = emptyList(),
) {
    init {
        require(displayName.isNotBlank())
        require(description.isNotBlank())
        require(role == TextRecognitionComponentRole.Detector || languages.isNotEmpty()) {
            "Text recognition component ${id.value} reads text but declares no language"
        }
        require(documentationUrl == null || documentationUrl.isNotBlank())
        require(scripts.all { languages.containsAll(it.languages) }) {
            "Text recognition component ${id.value} groups languages it does not read into scripts"
        }
    }
}

/** Languages a component reads with one model, named after the script they are written in. */
data class TextRecognitionScript(
    val displayName: String,
    val languages: Set<LanguageTag>,
) {
    init {
        require(displayName.isNotBlank())
        require(languages.isNotEmpty())
    }
}
