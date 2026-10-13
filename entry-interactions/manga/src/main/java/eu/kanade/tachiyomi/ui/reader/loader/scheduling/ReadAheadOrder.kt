package eu.kanade.tachiyomi.ui.reader.loader.scheduling

/**
 * The pages of a [pageCount] page chapter to load around the page at [index], in the order they should load: the
 * page, its neighbours, the pages ahead and then the pages behind. Without [wholeChapter] only the page before and
 * [LIMITED_PAGES_AHEAD] pages after load.
 */
internal fun readAheadOrder(index: Int, pageCount: Int, wholeChapter: Boolean): List<Int> {
    val window = if (wholeChapter) 0..<pageCount else (index - 1)..(index + LIMITED_PAGES_AHEAD)
    return buildList {
        add(index)
        add(index - 1)
        add(index + 1)
        addAll(index + 2..<pageCount)
        addAll(index - 2 downTo 0)
    }
        .filter { it in window && it in 0..<pageCount }
}

private const val LIMITED_PAGES_AHEAD = 4
