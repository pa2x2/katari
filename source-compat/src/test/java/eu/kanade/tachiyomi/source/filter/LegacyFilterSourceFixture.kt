package eu.kanade.tachiyomi.source.filter

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate

internal class LegacyFilterSourceFixture(
    var filters: () -> FilterList,
    var search: suspend (Int, String, FilterList) -> Unit,
) : Source {
    override val id = 1L
    override val name = "Legacy filters"
    override val supportsLatest = false

    override fun getFilterList() = filters()

    override suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage {
        search(page, query, filters)
        return MangasPage(emptyList(), false)
    }

    override suspend fun getPopularManga(page: Int): MangasPage = error("Unused")
    override suspend fun getLatestUpdates(page: Int): MangasPage = error("Unused")
    override suspend fun getMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): SMangaUpdate = error("Unused")
}
