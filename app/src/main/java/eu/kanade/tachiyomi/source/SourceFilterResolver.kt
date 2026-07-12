package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.source.adapter.LegacyMangaSourceAdapter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.UnifiedSource

// Compatibility overloads for already-installed legacy manga catalogue sources.
suspend fun CatalogueSource.resolveFilterList(): EntryFilterList {
    return when (this) {
        is AsyncCatalogueFilterSource -> getFilterListAsync().toEntryFilterList()
        else -> getFilterList().toEntryFilterList()
    }
}

fun CatalogueSource.defaultBackgroundFilterList(): EntryFilterList {
    return when (this) {
        is AsyncCatalogueFilterSource -> EntryFilterList()
        else -> getFilterList().toEntryFilterList()
    }
}

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
