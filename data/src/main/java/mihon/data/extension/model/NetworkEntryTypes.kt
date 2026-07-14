package mihon.data.extension.model

import eu.kanade.tachiyomi.source.entry.EntryType

internal fun List<String>.toSupportedEntryTypes(): Set<EntryType>? {
    return mapNotNullTo(linkedSetOf()) { value ->
        EntryType.entries.firstOrNull { value.equals(it.name, ignoreCase = true) }
    }
        .takeIf { it.isNotEmpty() }
}

internal fun String.legacyMangaEntryTypes(): Set<EntryType>? {
    val family = split('.').take(2).joinToString(".")
    return setOf(EntryType.MANGA).takeIf { family == "1.4" || family == "1.6" }
}
