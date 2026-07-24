package eu.kanade.tachiyomi.source.adapter

import eu.kanade.tachiyomi.source.AsyncCatalogueFilterSource
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.toEntryFilterList

fun UnifiedSource.hasAsyncFilters(): Boolean {
    return (this as? LegacyMangaSourceAdapter)?.source is AsyncCatalogueFilterSource
}

suspend fun UnifiedSource.resolveFilterList(): EntryFilterList {
    return when (val legacySource = (this as? LegacyMangaSourceAdapter)?.source) {
        is AsyncCatalogueFilterSource -> legacySource.getFilterListAsync().toEntryFilterList()
        else -> getFilterList()
    }
}

fun UnifiedSource.defaultBackgroundFilterList(): EntryFilterList {
    return if (hasAsyncFilters()) EntryFilterList() else getFilterList()
}

/** Returns legacy implementation identity without exposing the wrapped source as an application API. */
fun UnifiedSource.legacySourceClassName(): String? {
    return (this as? LegacyMangaSourceAdapter)?.source?.let { it::class.qualifiedName }
}
