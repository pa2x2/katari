package mihon.entry.interactions.book.reader.language

import mihon.language.api.identification.TextLanguageResolutionContext
import mihon.language.api.tag.LanguageTag

internal class BookSelectionLanguageSession(
    declaredLanguageTags: List<String>,
) {
    private val declaredLanguages = declaredLanguageTags.toLanguageTags()
    private var learnedLanguage: LanguageTag? = null

    /**
     * Languages the selected text declares for itself come before the publication's, so a quotation marked as
     * another language is not resolved as the book's language.
     */
    fun context(
        surroundingText: String,
        selectionLanguageTags: List<String>,
    ): TextLanguageResolutionContext {
        return TextLanguageResolutionContext(
            surroundingText = surroundingText,
            sessionLanguage = learnedLanguage,
            declaredLanguages = (selectionLanguageTags.toLanguageTags() + declaredLanguages).distinct(),
        )
    }

    fun record(language: LanguageTag) {
        learnedLanguage = language
    }
}

private fun List<String>.toLanguageTags(): List<LanguageTag> = mapNotNull(LanguageTag::parse).distinct()
