package eu.kanade.tachiyomi.source.lnreader.entry

import eu.kanade.tachiyomi.source.entry.ChapterListPart
import eu.kanade.tachiyomi.source.entry.ChapterListRest
import eu.kanade.tachiyomi.source.entry.EntryRefresh
import eu.kanade.tachiyomi.source.entry.SEntry
import eu.kanade.tachiyomi.source.entry.SEntryChapter
import eu.kanade.tachiyomi.source.lnreader.runtime.LnReaderPluginCalls
import eu.kanade.tachiyomi.source.lnreader.runtime.asArray
import eu.kanade.tachiyomi.source.lnreader.runtime.asObject
import eu.kanade.tachiyomi.source.lnreader.web.LnReaderUrls
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * A novel's details and chapters from `parseNovel`, and a paged list's pages from `parsePage`.
 *
 * Opening a novel asks for its details and then for its chapters, one `parseNovel` each, so a result is used again for
 * a few seconds rather than asked of the site twice.
 */
internal class LnReaderRefresh(
    private val calls: LnReaderPluginCalls,
    private val urls: LnReaderUrls,
    private val pagedChapters: Boolean,
) {
    private val recent = RecentNovels()

    suspend fun refresh(
        entry: SEntry,
        existingChapters: List<SEntryChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): EntryRefresh {
        val novel = novel(entry.url)
        val refreshed = if (fetchDetails) novel.details(entry, urls).withUrl() else entry
        if (!fetchChapters) return EntryRefresh(refreshed, existingChapters)

        val total = novel.totalPages
        if (!pagedChapters || total <= 1) {
            // A one-page list may still come only from parsePage, as LNReader asks for a page it has no chapters of
            val items = novel.chapters.takeIf { it.isNotEmpty() || !pagedChapters || total == 0 } ?: page(entry.url, 1)
            val list = lnReaderChapters(items)
            return EntryRefresh(refreshed, list.chapters.withUrls(), list.groups)
        }
        return refreshPages(refreshed, novel, existingChapters, total)
    }

    suspend fun olderChapters(entry: SEntry, token: String): ChapterListPart {
        val walk = PageWalk.of(token)
        val chapters = pageChapters(entry.url, walk.page, walk.newestFirst)
        val read = walk.read + 1
        return ChapterListPart(chapters, PageWalk.after(walk.page, walk.total, walk.newestFirst, walk.last, read))
    }

    private suspend fun refreshPages(
        entry: SEntry,
        novel: LnReaderNovel,
        existingChapters: List<SEntryChapter>,
        total: Int,
    ): EntryRefresh {
        val stored = existingChapters.mapNotNull(ChapterPage::of)
        val storedTotal = stored.maxOfOrNull(ChapterPage::number)
        if (storedTotal != null && total >= storedTotal) {
            // A library update: the newest page, and the pages the list grew by
            val newestFirst = stored.any(ChapterPage::newestFirst)
            val newest = if (newestFirst) 1 else total
            val chapters = pageChapters(
                entry.url,
                newest,
                newestFirst,
                novel.chapters.takeIf { newestFirst && it.isNotEmpty() },
            )
            val storedUrls = existingChapters.mapTo(HashSet(), SEntryChapter::url)
            // Page 1 of a newest-first site takes new chapters in front, pushing older ones on by as many pages
            val last = when {
                !newestFirst -> storedTotal
                chapters.any { it.url in storedUrls } -> newest
                else -> minOf(total, 2 + total - storedTotal)
            }
            return EntryRefresh(entry, chapters, rest = PageWalk.after(newest, total, newestFirst, last, read = 1))
        }

        // The first walk: which end is newest decides where it starts
        val first = novel.chapters.takeIf { it.isNotEmpty() } ?: page(entry.url, 1)
        val lastPage = page(entry.url, total)
        val newestFirst = (highestNumber(first) ?: 0.0) > (highestNumber(lastPage) ?: Double.MAX_VALUE)
        val newest = if (newestFirst) 1 else total
        val chapters = lnReaderChapters(if (newestFirst) first else lastPage, ChapterPage(newest, newestFirst))
            .chapters
            .withUrls()
        return EntryRefresh(entry, chapters, rest = PageWalk.after(newest, total, newestFirst, last = null, read = 1))
    }

    private suspend fun pageChapters(
        path: String,
        page: Int,
        newestFirst: Boolean,
        items: JsonArray? = null,
    ): List<SEntryChapter> =
        lnReaderChapters(items ?: page(path, page), ChapterPage(page, newestFirst)).chapters.withUrls()

    private suspend fun novel(path: String): LnReaderNovel {
        val profileId = calls.profileId
        recent.get(profileId, path)?.let { return it }
        val novel = LnReaderNovel(calls.call("parseNovel", JsonPrimitive(path)).asObject() ?: JsonObject(emptyMap()))
        recent.put(profileId, path, novel)
        return novel
    }

    private suspend fun page(path: String, page: Int): JsonArray =
        calls.call(
            "parsePage",
            JsonPrimitive(path),
            JsonPrimitive(page.toString()),
        ).asObject()?.get("chapters").asArray()

    private suspend fun SEntry.withUrl(): SEntry = also {
        it.memo = it.memo.withUrl(urls.resolve(listOf(url), isNovel = true).single())
    }

    private suspend fun List<SEntryChapter>.withUrls(): List<SEntryChapter> {
        urls.resolve(map(SEntryChapter::url), isNovel = false).forEachIndexed { index, url ->
            this[index].memo = this[index].memo.withUrl(url)
        }
        return this
    }

    private fun JsonObject.withUrl(url: String?): JsonObject =
        JsonObject(
            if (url ==
                null
            ) {
                this - LnReaderUrls.MEMO_URL
            } else {
                this + (LnReaderUrls.MEMO_URL to JsonPrimitive(url))
            },
        )
}

/** The last few `parseNovel` results per profile, whose settings can change what a plugin gives. */
private class RecentNovels {
    private class Recent(val novel: LnReaderNovel, val at: TimeSource.Monotonic.ValueTimeMark)

    private val novels = object : LinkedHashMap<Pair<Long, String>, Recent>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Pair<Long, String>, Recent>) = size > SIZE
    }

    fun get(profileId: Long, path: String): LnReaderNovel? = synchronized(novels) {
        novels[profileId to path]?.takeIf { it.at.elapsedNow() < REUSE }?.novel
    }

    fun put(profileId: Long, path: String, novel: LnReaderNovel) = synchronized(novels) {
        novels[profileId to path] = Recent(novel, TimeSource.Monotonic.markNow())
    }

    private companion object {
        const val SIZE = 8
        val REUSE = 30.seconds
    }
}
