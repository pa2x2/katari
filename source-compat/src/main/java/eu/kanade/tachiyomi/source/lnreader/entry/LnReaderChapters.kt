package eu.kanade.tachiyomi.source.lnreader.entry

import eu.kanade.tachiyomi.source.entry.SEntryChapter
import eu.kanade.tachiyomi.source.entry.SEntryChapterGroup
import eu.kanade.tachiyomi.source.lnreader.runtime.asNumber
import eu.kanade.tachiyomi.source.lnreader.runtime.asObject
import eu.kanade.tachiyomi.source.lnreader.runtime.asText
import eu.kanade.tachiyomi.source.lnreader.runtime.text
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/** Chapters newest first, as Katari lists them, and the groups they belong to in reading order. */
internal class LnReaderChapterList(val chapters: List<SEntryChapter>, val groups: List<SEntryChapterGroup>)

/**
 * A plugin's chapter items as Katari's chapters. Plugins list chapters oldest first, so the list is turned around,
 * unless its chapter numbers mostly fall from one to the next, as on a few sites' pages.
 *
 * A list read whole is grouped by the items' `page` labels when it has two or more, as a volume or a book of the story;
 * a list read in pages ([page] set) is not, its labels being the pages, which each chapter's memo keeps instead. A path
 * listed twice is kept once.
 */
internal fun lnReaderChapters(items: JsonArray, page: ChapterPage? = null): LnReaderChapterList {
    val seen = HashSet<String>()
    val listed = items.mapNotNull { it.asObject() }.filter { item -> item.text("path")?.let(seen::add) == true }
    val labels = if (page == null) listed.map { it.text("page")?.trim()?.takeIf(String::isNotEmpty) } else emptyList()
    val groupKeys = labels.filterNotNull().distinct().takeIf { it.size >= 2 }.orEmpty().toSet()
    val chapters = listed.mapIndexed { index, item ->
        item.toChapter(position = index + 1, group = labels.getOrNull(index)?.takeIf(groupKeys::contains), page = page)
    }
    return LnReaderChapterList(
        chapters = if (runsNewestFirst(listed)) chapters else chapters.asReversed(),
        groups = groupKeys.map { SEntryChapterGroup.create(it, it) },
    )
}

private fun JsonObject.toChapter(position: Int, group: String?, page: ChapterPage?): SEntryChapter =
    SEntryChapter.create().also { chapter ->
        chapter.url = text("path")!!
        chapter.name = text("name")?.trim()?.takeIf(String::isNotEmpty) ?: "Chapter $position"
        chapter.chapterNumber = number() ?: -1.0
        chapter.scanlator = when (val scanlator = get("scanlator")) {
            is JsonArray -> scanlator.mapNotNull { it.asText() }.joinToString(", ")
            else -> scanlator.asText()
        }?.takeIf(String::isNotBlank)
        lnReaderReleaseTime(get("releaseTime"))?.let {
            chapter.dateUpload = it.date
            chapter.dateText = it.text
        }
        chapter.group = group
        if (page != null) chapter.memo = page.memo
    }

private fun JsonObject.number(): Double? =
    (get("chapterNumber").asNumber() ?: text("chapterNumber")?.toDoubleOrNull())?.takeIf { it >= 0 }

// By the numbers of neighbouring chapters, so a stray number at either end, such as a trailing 0, doesn't decide it
private fun runsNewestFirst(items: List<JsonObject>): Boolean {
    val numbers = items.mapNotNull(::orderNumber)
    val steps = numbers.zipWithNext { older, newer -> newer.compareTo(older) }
    return steps.count { it < 0 } > steps.count { it > 0 }
}

/** The highest chapter number among [items], to tell which end of a paged list is newer. */
internal fun highestNumber(
    items: JsonArray,
): Double? = items.mapNotNull { it.asObject()?.let(::orderNumber) }.maxOrNull()

// The chapter's own number, else the first number in its name
private fun orderNumber(item: JsonObject): Double? =
    item.number() ?: item.text("name")?.let { NUMBER.find(it)?.value?.toDoubleOrNull() }

private val NUMBER = Regex("""\d+(?:\.\d+)?""")
