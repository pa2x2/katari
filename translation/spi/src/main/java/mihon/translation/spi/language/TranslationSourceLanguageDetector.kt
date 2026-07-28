package mihon.translation.spi

import mihon.translation.api.TranslationLanguageTag

@JvmInline
value class TranslationSourceLanguageDetectorId(
    val value: String,
) {
    init {
        require(value.isNotBlank())
    }
}

interface TranslationSourceLanguageDetector {
    val id: TranslationSourceLanguageDetectorId

    suspend fun detect(text: String): TranslationSourceLanguageDetection
}

sealed interface TranslationSourceLanguageDetection {
    data class Detected(
        val language: TranslationLanguageTag,
        val confidence: Float? = null,
    ) : TranslationSourceLanguageDetection {
        init {
            require(confidence == null || confidence in 0f..1f)
        }
    }

    data object Undetermined : TranslationSourceLanguageDetection

    data class Unavailable(
        val reason: String,
    ) : TranslationSourceLanguageDetection {
        init {
            require(reason.isNotBlank())
        }
    }
}
