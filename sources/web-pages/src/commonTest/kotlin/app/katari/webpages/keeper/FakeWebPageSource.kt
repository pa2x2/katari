package app.katari.webpages.keeper

import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.EntryPageResult
import eu.kanade.tachiyomi.source.entry.EntryRefresh
import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import eu.kanade.tachiyomi.source.entry.SEntry
import eu.kanade.tachiyomi.source.entry.SEntryChapter
import eu.kanade.tachiyomi.source.entry.webpage.WebPage
import eu.kanade.tachiyomi.source.entry.webpage.WebPageSource
import eu.kanade.tachiyomi.source.entry.webpage.WebPageSpec

/** A source that only keeps pages, counting how often it prepared one. */
internal class FakeWebPageSource(override val id: Long) : WebPageSource {
    override val name = "Source $id"

    var prepared = 0
        private set

    override fun webPageSpec(name: String) = WebPageSpec(url = "https://site.example/")

    override suspend fun prepareWebPage(name: String, page: WebPage) {
        prepared++
    }

    override suspend fun getPopularContent(page: Int): EntryPageResult<SEntry> = unused()

    override suspend fun getLatestUpdates(page: Int): EntryPageResult<SEntry> = unused()

    override suspend fun getSearchContent(page: Int, query: String, filters: EntryFilterList): EntryPageResult<SEntry> =
        unused()

    override suspend fun refresh(
        entry: SEntry,
        existingChapters: List<SEntryChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): EntryRefresh = unused()

    override suspend fun getMedia(chapter: SEntryChapter, selection: PlaybackSelection): EntryMedia = unused()

    private fun unused(): Nothing = error("Not part of keeping pages")
}
