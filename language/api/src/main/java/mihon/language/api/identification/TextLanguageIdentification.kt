package mihon.language.api.identification

import mihon.language.api.tag.LanguageTag

@JvmInline
value class TextLanguageDetectorId(
    val value: String,
) {
    init {
        require(ID_PATTERN.matches(value)) { "Invalid text language detector id: '$value'" }
    }

    private companion object {
        val ID_PATTERN = Regex("""[a-z][a-z0-9]*(?:[.-][a-z0-9]+)*""")
    }
}

/** Identifies text language without assigning product behavior to the detector. */
interface TextLanguageDetector {
    val id: TextLanguageDetectorId

    suspend fun detect(text: String): TextLanguageDetection
}

data class TextLanguageCandidate(
    val language: LanguageTag,
    val confidence: Float? = null,
) {
    init {
        require(confidence == null || confidence in 0f..1f)
    }
}

data class TextLanguageResolutionContext(
    val surroundingText: String? = null,
    val sessionLanguage: LanguageTag? = null,
    val declaredLanguages: List<LanguageTag> = emptyList(),
) {
    init {
        require(surroundingText == null || surroundingText.isNotBlank())
        require(declaredLanguages.distinct().size == declaredLanguages.size)
    }
}

sealed interface TextLanguageDetection {
    data class Detected(
        val language: LanguageTag,
        val confidence: Float? = null,
        val alternatives: List<TextLanguageCandidate> = emptyList(),
    ) : TextLanguageDetection {
        init {
            require(confidence == null || confidence in 0f..1f)
            require(alternatives.none { it.language == language })
            require(alternatives.distinctBy(TextLanguageCandidate::language).size == alternatives.size)
        }

        val candidates: List<TextLanguageCandidate>
            get() = listOf(TextLanguageCandidate(language, confidence)) + alternatives
    }

    data object Undetermined : TextLanguageDetection

    data class Unavailable(
        val reason: String,
    ) : TextLanguageDetection {
        init {
            require(reason.isNotBlank())
        }
    }
}
