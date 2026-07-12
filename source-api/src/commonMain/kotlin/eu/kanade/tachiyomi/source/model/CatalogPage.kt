package eu.kanade.tachiyomi.source.model

data class CatalogPage(val items: List<CatalogItem>, val hasNextPage: Boolean)

fun CatalogPage.toMangasPage(): MangasPage =
    MangasPage(items.filterIsInstance<CatalogItem.MangaItem>().map { it.manga }, hasNextPage)
