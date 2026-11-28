package eu.kanade.tachiyomi.data.backup.conversion.lnreader.settings

import tachiyomi.domain.entry.model.Entry

/**
 * LNReader's chapter orders (CHAPTER_ORDER of src/database/constants.ts) as Katari's chapter sort; [sorting] is null for
 * one Katari can't sort by. Katari's source order counts from the newest chapter, so LNReader's position order, which
 * counts from the oldest, keeps its direction under Katari's source sort.
 */
internal enum class LnReaderChapterOrder(private val key: String, private val sql: String, val sorting: Long?) {
    READ_TIME_ASC("readTimeAsc", "readTime ASC", null),
    READ_TIME_DESC("readTimeDesc", "readTime DESC", null),
    POSITION_ASC("positionAsc", "position ASC", Entry.CHAPTER_SORTING_SOURCE),
    POSITION_DESC("positionDesc", "position DESC", Entry.CHAPTER_SORTING_SOURCE),
    NAME_ASC("nameAsc", "name ASC", Entry.CHAPTER_SORTING_ALPHABET),
    NAME_DESC("nameDesc", "name DESC", Entry.CHAPTER_SORTING_ALPHABET),
    ;

    val direction: Long get() = if (sql.endsWith(" ASC")) Entry.CHAPTER_SORT_ASC else Entry.CHAPTER_SORT_DESC

    companion object {
        // LNReader's first default, and what it sorts by when its default chapter sort is unset
        val default = POSITION_ASC

        /** The order [value] names: its key, or the SQL LNReader kept before 2.0 (`ORDER BY position ASC`). */
        fun of(value: String): LnReaderChapterOrder? = entries.find { value == it.key || value == "ORDER BY ${it.sql}" }
    }
}

/**
 * A novel's chapter list as LNReader showed it (D092): its own [stored] settings, and the app's [defaultOrder] where
 * they set none. An order Katari can't sort by gives way to the next, and last to Katari's default.
 */
internal class LnReaderChapterList(stored: LnReaderNovelSettings?, defaultOrder: LnReaderChapterOrder) {
    private val ownOrder = stored?.sort?.let(LnReaderChapterOrder::of)

    /** Whether the novel's own order is one Katari can't sort by. */
    val unsortable: Boolean = ownOrder != null && ownOrder.sorting == null

    val chapterFlags: Long = run {
        val sort = listOfNotNull(ownOrder, defaultOrder)
            .firstNotNullOfOrNull { order -> order.sorting?.let { it or order.direction } }
            ?: DEFAULT_SORT
        val filters = stored?.filter.orEmpty().fold(0L) { flags, filter -> flags or (FILTERS[filter] ?: 0L) }
        // Without titles LNReader names each chapter by its number
        val titlesOff = stored?.showChapterTitles == false
        val display = if (titlesOff) Entry.CHAPTER_DISPLAY_NUMBER else Entry.CHAPTER_DISPLAY_NAME
        sort or filters or display
    }

    val excludedScanlators: List<String> = stored?.excludedScanlators.orEmpty()

    private companion object {
        val DEFAULT_SORT = Entry.DEFAULT_CHAPTER_FLAGS and (Entry.CHAPTER_SORTING_MASK or Entry.CHAPTER_SORT_DIR_MASK)

        // ChapterFilterKey of src/database/constants.ts: each keeps only the chapters it names
        val FILTERS = mapOf(
            "read" to Entry.CHAPTER_SHOW_READ,
            "not-read" to Entry.CHAPTER_SHOW_UNREAD,
            "downloaded" to Entry.CHAPTER_SHOW_DOWNLOADED,
            "not-downloaded" to Entry.CHAPTER_SHOW_NOT_DOWNLOADED,
            "bookmarked" to Entry.CHAPTER_SHOW_BOOKMARKED,
            "not-bookmarked" to Entry.CHAPTER_SHOW_NOT_BOOKMARKED,
        )
    }
}
