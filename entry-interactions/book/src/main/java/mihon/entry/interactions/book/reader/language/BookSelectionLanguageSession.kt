package mihon.entry.interactions.book.reader.language

import mihon.language.api.identification.TextLanguageResolutionContext
import mihon.language.api.tag.LanguageTag

internal class BookSelectionLanguageSession(
    declaredLanguageTags: List<String>,
) {
    private val declaredLanguages = declaredLanguageTags
        .mapNotNull(LanguageTag::parse)
        .distinct()
    private var learnedLanguage: LanguageTag? = null

    fun context(surroundingText: String): TextLanguageResolutionContext {
        return TextLanguageResolutionContext(
            surroundingText = surroundingText,
            sessionLanguage = learnedLanguage,
            declaredLanguages = declaredLanguages,
        )
    }

    fun record(language: LanguageTag) {
        learnedLanguage = language
    }
}
