package mihon.entry.interactions.manga.reader.text.session

import mihon.language.api.tag.LanguageTag

/**
 * The language a source declares for its content, or `null` when the source spans several languages and the reader
 * has to ask.
 */
internal fun declaredContentLanguage(sourceLanguage: String?): LanguageTag? {
    val value = sourceLanguage?.trim()?.takeIf(String::isNotEmpty) ?: return null
    if (value.lowercase() in MULTI_LANGUAGE_CODES) return null
    return LanguageTag.parse(value)
}

/** Codes extension sources use for catalogs that are not in a single language. */
private val MULTI_LANGUAGE_CODES = setOf("all", "multi", "other", "mul", "und")
