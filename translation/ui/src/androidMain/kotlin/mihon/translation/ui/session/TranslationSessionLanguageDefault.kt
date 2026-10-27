package mihon.translation.ui.session

import mihon.language.api.tag.LanguageTag
import mihon.translation.api.language.TranslationDefaultTarget

/** What a language picker's first row follows instead of a pinned language, and whether the session follows it. */
sealed interface TranslationSessionLanguageDefault {
    val selected: Boolean

    data class Target(
        val target: TranslationDefaultTarget,
        override val selected: Boolean,
    ) : TranslationSessionLanguageDefault

    /** Detecting the source, with the language [detected] for the text and the one the text [declared] for itself. */
    data class AutomaticSource(
        val detected: LanguageTag?,
        val declared: LanguageTag?,
        override val selected: Boolean,
    ) : TranslationSessionLanguageDefault
}
