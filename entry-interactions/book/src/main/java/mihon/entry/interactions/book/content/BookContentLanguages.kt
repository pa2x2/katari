package mihon.entry.interactions.book.content

import mihon.language.api.tag.LanguageTag

internal fun Iterable<String>.normalizedBookContentLanguages(): List<String> {
    return mapNotNull { candidate ->
        candidate
            .takeUnless { it.trim().lowercase() in NON_LANGUAGE_SOURCE_TAGS }
            ?.let(LanguageTag::parse)
            ?.value
    }.distinct()
}

private val NON_LANGUAGE_SOURCE_TAGS = setOf("all", "other")
