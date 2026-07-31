package mihon.translation.api.language

data class TranslationLanguagePair(
    val source: TranslationLanguageTag,
    val target: TranslationLanguageTag,
) {
    init {
        require(source != target) { "Translation language pair must contain different languages" }
    }
}

/**
 * Provider-owned language support exposed to Translation selection surfaces.
 *
 * Providers should expose exact pairs when possible, fall back to role-specific
 * language sets when pair topology is unavailable, and use [AnyLanguage] only
 * when they intentionally accept arbitrary determinate BCP-47 tags.
 */
sealed interface TranslationLanguageSupport {
    data class ExactPairs(
        val pairs: Set<TranslationLanguagePair>,
    ) : TranslationLanguageSupport {
        init {
            require(pairs.isNotEmpty()) { "Exact Translation language support must not be empty" }
        }
    }

    data class ByRole(
        val sourceLanguages: Set<TranslationLanguageTag>,
        val targetLanguages: Set<TranslationLanguageTag>,
    ) : TranslationLanguageSupport {
        init {
            require(sourceLanguages.isNotEmpty()) {
                "Role-based Translation source language support must not be empty"
            }
            require(targetLanguages.isNotEmpty()) {
                "Role-based Translation target language support must not be empty"
            }
        }
    }

    data object AnyLanguage : TranslationLanguageSupport
}

sealed interface TranslationLanguageSupportInspection {
    data class Available(
        val support: TranslationLanguageSupport,
    ) : TranslationLanguageSupportInspection

    data class Unavailable(
        val reason: String? = null,
    ) : TranslationLanguageSupportInspection {
        init {
            require(reason == null || reason.isNotBlank())
        }
    }
}
