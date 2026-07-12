package tachiyomi.domain.source.service

import eu.kanade.tachiyomi.source.entry.EntryCatalogueSource

sealed class CatalogSource {
    abstract val source: EntryCatalogueSource

    data class Mixed(override val source: EntryCatalogueSource) : CatalogSource()
}
