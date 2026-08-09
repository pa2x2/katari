package mihon.language.api.tag

import java.util.Locale

/** A determinate, normalized BCP-47 language tag. */
@JvmInline
value class LanguageTag private constructor(
    val value: String,
) {
    companion object {
        fun parse(value: String): LanguageTag? {
            val candidate = value.trim().replace('_', '-')
            if (candidate.isEmpty()) return null

            val locale = Locale.forLanguageTag(candidate)
            if (locale.language.isEmpty() || locale.language == UNDETERMINED_LANGUAGE) return null

            val normalized = locale.toLanguageTag()
            return normalized
                .takeUnless { it.isEmpty() || it == UNDETERMINED_LANGUAGE }
                ?.let(::LanguageTag)
        }

        fun require(value: String): LanguageTag = requireNotNull(parse(value)) {
            "Language tag must be a determinate BCP-47 tag: '$value'"
        }

        private const val UNDETERMINED_LANGUAGE = "und"
    }
}
