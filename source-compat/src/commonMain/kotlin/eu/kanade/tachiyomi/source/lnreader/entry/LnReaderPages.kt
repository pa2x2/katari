package eu.kanade.tachiyomi.source.lnreader.entry

import eu.kanade.tachiyomi.source.entry.ChapterListRest
import eu.kanade.tachiyomi.source.entry.SEntryChapter
import eu.kanade.tachiyomi.source.lnreader.runtime.asBoolean
import eu.kanade.tachiyomi.source.lnreader.runtime.asNumber
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/*
 * Chapter lists a site splits into pages (`totalPages`, each read with `parsePage`) are read in parts: the newest page
 * with the refresh, then each older one in the background.
 *
 * Plugins put the oldest chapters on page 1, but a few sites put the newest there. Which a novel does is found once, by
 * comparing the chapter numbers of its first and last pages, and kept in the memo of every chapter beside its page, as
 * the host keeps an entry's memo only from a details refresh. A library update then reads pages as LNReader does:
 * the newest page, and when the list grew by pages, the new ones down to the one that was newest; the chapters before
 * stay as stored.
 */

/** A chapter's page of a paged list, and whether the site lists its newest chapters on page 1. */
internal class ChapterPage(val number: Int, val newestFirst: Boolean) {
    val memo: JsonObject
        get() = JsonObject(
            buildMap {
                put(MEMO_PAGE, JsonPrimitive(number))
                if (newestFirst) put(MEMO_NEWEST_PAGE_FIRST, JsonPrimitive(true))
            },
        )

    companion object {
        fun of(chapter: SEntryChapter): ChapterPage? {
            val number = chapter.memo[MEMO_PAGE].asNumber()?.toInt() ?: return null
            return ChapterPage(number, chapter.memo[MEMO_NEWEST_PAGE_FIRST].asBoolean() == true)
        }
    }
}

/**
 * Where a walk through a paged list stands: [page] is read next, of [total], newest first. A walk with [last] reads
 * up to that page and leaves the older chapters as stored; without it, it reads the whole list. [read] counts the pages
 * read before.
 */
@Serializable
internal class PageWalk(
    val page: Int,
    val total: Int,
    val newestFirst: Boolean,
    val last: Int? = null,
    val read: Int = 1,
) {
    fun token(): String = Json.encodeToString(serializer(), this)

    companion object {
        fun of(token: String): PageWalk = Json.decodeFromString(serializer(), token)

        /**
         * What follows page [page] of [total], [read] pages into a walk that ends at [last], or at the oldest page when
         * it's null.
         */
        fun after(page: Int, total: Int, newestFirst: Boolean, last: Int?, read: Int): ChapterListRest {
            if (last != null && page == last) return ChapterListRest.Unchanged
            val older = if (newestFirst) page + 1 else page - 1
            if (older !in 1..total) return ChapterListRest.Complete
            val walk = PageWalk(older, total, newestFirst, last, read)
            return ChapterListRest.More(
                walk.token(),
                loaded = read.takeIf { last == null },
                total = total.takeIf {
                    last ==
                        null
                },
            )
        }
    }
}

private const val MEMO_PAGE = "page"
private const val MEMO_NEWEST_PAGE_FIRST = "newestPageFirst"
