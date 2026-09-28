package mihon.text.recognition.api.provider

@JvmInline
value class TextRecognitionProviderId(
    val value: String,
) {
    init {
        require(ID_PATTERN.matches(value)) { "Invalid text recognition provider id: '$value'" }
    }

    private companion object {
        val ID_PATTERN = Regex("""[a-z][a-z0-9]*(?:[.-][a-z0-9]+)*""")
    }
}

/**
 * A recognition engine the profile can choose: the owner of a set of components and the presets that combine them.
 * A provider stays in the catalog of builds that exclude it, so hosts can explain why it cannot be chosen.
 *
 * @property bestFor the pages the engine reads best, to tell engines apart at a glance.
 */
data class KnownTextRecognitionProvider(
    val id: TextRecognitionProviderId,
    val name: String,
    val description: String,
    val processingLocation: String,
    val buildAvailability: TextRecognitionBuildAvailability,
    val documentationUrl: String? = null,
    val bestFor: String? = null,
) {
    init {
        require(name.isNotBlank())
        require(description.isNotBlank())
        require(processingLocation.isNotBlank())
        require(documentationUrl == null || documentationUrl.isNotBlank())
        require(bestFor == null || bestFor.isNotBlank())
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
