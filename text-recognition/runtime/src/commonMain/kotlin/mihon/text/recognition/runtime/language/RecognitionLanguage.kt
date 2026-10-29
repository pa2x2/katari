package mihon.text.recognition.runtime.language

import mihon.language.api.tag.LanguageTag
import java.util.Locale

/**
 * Recognition models read scripts, not regional variants, so components, presets, and profile choices are matched by
 * primary language subtag.
 */
internal val LanguageTag.recognitionLanguage: String
    get() = value.substringBefore('-').lowercase(Locale.ROOT)

internal fun Set<LanguageTag>.readsLanguage(language: LanguageTag): Boolean =
    any { it.recognitionLanguage == language.recognitionLanguage }

/** Manga published in Japanese is laid out right to left; the other supported layouts read left to right. */
internal val LanguageTag.readsRightToLeft: Boolean
    get() = recognitionLanguage == "ja"
