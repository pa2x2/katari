package eu.kanade.tachiyomi.source.adapter

import eu.kanade.tachiyomi.source.entry.UnifiedSource

/** Returns legacy implementation identity without exposing the wrapped source as an application API. */
fun UnifiedSource.legacySourceClassName(): String? {
    return (this as? LegacyMangaSourceAdapter)?.source?.let { it::class.qualifiedName }
}
