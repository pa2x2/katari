package mihon.entry.interactions.anime.download

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.util.storage.DiskUtil
import mihon.entry.interactions.catalogue.EntryCatalogueSourceInfo
import mihon.entry.interactions.download.SourceDirNames

/**
 * `<name> (<LANG>) [anime]`, beside a manga source's `<name> (<LANG>)` in the same downloads folder. Before the language
 * was added, every language of a multi-language extension wrote to the legacy `<name> [anime]`.
 */
internal fun animeSourceDirNames(name: String, language: String?, disallowNonAscii: Boolean): SourceDirNames {
    val legacy = DiskUtil.buildValidFilename("$name [anime]", disallowNonAscii = disallowNonAscii)
    if (language.isNullOrBlank()) return SourceDirNames(current = legacy, legacy = null)
    val current = DiskUtil.buildValidFilename(
        "$name (${language.uppercase()}) [anime]",
        disallowNonAscii = disallowNonAscii,
    )
    return SourceDirNames(current = current, legacy = legacy)
}

/**
 * The folder names of every source whose anime downloads a scan of the downloads folder looks for, by source key. A
 * source that says it supplies no anime has none: a same-named manga source would otherwise claim the anime folder.
 */
internal fun List<EntryCatalogueSourceInfo>.animeSourceDirNames(disallowNonAscii: Boolean): Map<Long, SourceDirNames> {
    return filter { it.supportedEntryTypes?.contains(EntryType.ANIME) != false }
        .associate { it.id to animeSourceDirNames(it.name, it.language, disallowNonAscii) }
}
