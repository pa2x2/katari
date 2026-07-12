package eu.kanade.tachiyomi.source.model

sealed interface CatalogItem {
    val title: String
    val thumbnailUrl: String?

    data class MangaItem(val manga: SManga) : CatalogItem {
        override val title: String get() = manga.title
        override val thumbnailUrl: String? get() = manga.thumbnail_url
    }
}
