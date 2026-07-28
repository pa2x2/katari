package mihon.translation.provider.libretranslate.protocol

import mihon.translation.api.TranslationLanguageTag
import java.util.Locale

internal class LibreTranslateLanguageResolver(
    private val languages: List<LibreTranslateLanguage>,
) {
    fun resolve(language: TranslationLanguageTag): LibreTranslateLanguage? {
        val exact = languages.filter { candidate ->
            candidate.normalizedTag()?.equals(language.value, ignoreCase = true) == true
        }
        if (exact.size == 1) return exact.single()
        if (exact.isNotEmpty()) return null

        val requestedBase = Locale.forLanguageTag(language.value).language
        return languages
            .filter { candidate -> candidate.normalizedTag()?.languageCode() == requestedBase }
            .singleOrNull()
    }

    fun supportsTarget(
        source: LibreTranslateLanguage,
        target: LibreTranslateLanguage,
    ): Boolean {
        val targetTag = target.normalizedTag()
        return source.targets.any { candidate ->
            candidate.equals(target.code, ignoreCase = true) ||
                (
                    targetTag != null &&
                        TranslationLanguageTag.parse(candidate)?.value
                            ?.equals(targetTag, ignoreCase = true) == true
                    )
        }
    }

    private fun LibreTranslateLanguage.normalizedTag(): String? {
        return TranslationLanguageTag.parse(code)?.value
    }

    private fun String.languageCode(): String = Locale.forLanguageTag(this).language
}
