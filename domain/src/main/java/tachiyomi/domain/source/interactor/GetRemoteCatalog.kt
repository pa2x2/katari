package tachiyomi.domain.source.interactor

import eu.kanade.tachiyomi.source.entry.EntryFilterList
import tachiyomi.domain.source.repository.CatalogPagingSource
import tachiyomi.domain.source.repository.CatalogSourceRepository

class GetRemoteCatalog(
    private val repository: CatalogSourceRepository,
) {

    operator fun invoke(sourceId: Long, query: String, filterList: EntryFilterList): CatalogPagingSource {
        return when (query) {
            QUERY_POPULAR -> repository.getPopular(sourceId)
            QUERY_LATEST -> repository.getLatest(sourceId)
            // Backward-compatible with the constants used by the legacy manga/anime browse screens.
            LEGACY_MANGA_QUERY_POPULAR -> repository.getPopular(sourceId)
            LEGACY_MANGA_QUERY_LATEST -> repository.getLatest(sourceId)
            LEGACY_ANIME_QUERY_POPULAR -> repository.getPopular(sourceId)
            LEGACY_ANIME_QUERY_LATEST -> repository.getLatest(sourceId)
            else -> repository.search(sourceId, query, filterList)
        }
    }

    companion object {
        const val QUERY_POPULAR = "eu.kanade.domain.source.interactor.CATALOG_POPULAR"
        const val QUERY_LATEST = "eu.kanade.domain.source.interactor.CATALOG_LATEST"

        private const val LEGACY_MANGA_QUERY_POPULAR = "eu.kanade.domain.source.interactor.POPULAR"
        private const val LEGACY_MANGA_QUERY_LATEST = "eu.kanade.domain.source.interactor.LATEST"
        private const val LEGACY_ANIME_QUERY_POPULAR = "eu.kanade.domain.source.interactor.VIDEO_POPULAR"
        private const val LEGACY_ANIME_QUERY_LATEST = "eu.kanade.domain.source.interactor.VIDEO_LATEST"
    }
}
