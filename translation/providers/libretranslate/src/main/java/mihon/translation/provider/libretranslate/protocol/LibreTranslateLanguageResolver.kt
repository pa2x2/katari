package mihon.translation.provider.libretranslate.protocol

import mihon.translation.api.TranslationLanguagePair
import mihon.translation.api.TranslationLanguageSupport
import mihon.translation.api.TranslationLanguageSupportInspection
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

    fun languageSupport(): TranslationLanguageSupportInspection {
        val resolvable = languages.mapNotNull { language ->
            val tag = TranslationLanguageTag.parse(language.code) ?: return@mapNotNull null
            (language to tag).takeIf { resolve(tag) == language }
        }
        val pairs = resolvable.flatMapTo(mutableSetOf()) { (source, sourceTag) ->
            resolvable.mapNotNull { (target, targetTag) ->
                if (sourceTag == targetTag || !supportsTarget(source, target)) {
                    null
                } else {
                    TranslationLanguagePair(sourceTag, targetTag)
                }
            }
        }
        return if (pairs.isEmpty()) {
            TranslationLanguageSupportInspection.Unavailable(
                "Provider reported no supported translation language pairs",
            )
        } else {
            TranslationLanguageSupportInspection.Available(
                TranslationLanguageSupport.ExactPairs(pairs),
            )
        }
    }

    private fun LibreTranslateLanguage.normalizedTag(): String? {
        return TranslationLanguageTag.parse(code)?.value
    }

    private fun String.languageCode(): String = Locale.forLanguageTag(this).language
}
