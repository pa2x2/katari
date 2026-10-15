package mihon.translation.ui.picker.language

import java.text.Normalizer

internal fun String.normalizedForSearch(): String = Normalizer
    .normalize(this, Normalizer.Form.NFD)
    .replace(diacriticalMarks, "")
    .lowercase()

internal fun List<TranslationLanguageOption>.matching(normalizedQuery: String): List<TranslationLanguageOption> =
    if (normalizedQuery.isEmpty()) this else filter { option -> option.matches(normalizedQuery) }

/**
 * Search matches the localized name, the language's own native name, and the raw tag, so a query
 * succeeds regardless of which name the user knows the language by. Accents are ignored, so an
 * ASCII query such as "espanol" still finds "Español".
 */
private fun TranslationLanguageOption.matches(normalizedQuery: String): Boolean =
    displayName.normalizedForSearch().contains(normalizedQuery) ||
        nativeName.normalizedForSearch().contains(normalizedQuery) ||
        tag.value.contains(normalizedQuery, ignoreCase = true)

private val diacriticalMarks = Regex("\\p{Mn}+")
