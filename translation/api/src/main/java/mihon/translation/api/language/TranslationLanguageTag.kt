package mihon.translation.api.language

import java.util.Locale

/**
 * Provider-neutral BCP-47 language identity.
 *
 * Provider enums and Android locale objects are converted at adapter boundaries and never cross the Translation API.
 */
@JvmInline
value class TranslationLanguageTag private constructor(
    val value: String,
) {
    companion object {
        fun parse(value: String): TranslationLanguageTag? {
            val candidate = value.trim().replace('_', '-')
            if (candidate.isEmpty()) return null

            val locale = Locale.forLanguageTag(candidate)
            if (locale.language.isEmpty() || locale.language == UNDETERMINED_LANGUAGE) return null

            val normalized = locale.toLanguageTag()
            return normalized
                .takeUnless { it.isEmpty() || it == UNDETERMINED_LANGUAGE }
                ?.let(::TranslationLanguageTag)
        }

        fun require(value: String): TranslationLanguageTag {
            return requireNotNull(parse(value)) {
                "Translation language tag must be a determinate BCP-47 tag: '$value'"
            }
        }

        private const val UNDETERMINED_LANGUAGE = "und"
    }
}
