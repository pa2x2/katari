package eu.kanade.tachiyomi.ui.reader.loader.scheduling

internal class PageLoadChanges<T>(
    val start: List<T>,
    val cancel: List<T>,
)

/**
 * Picks the pages that download now: the first [slots] pages of [wanted] that are [loading] or still [missing].
 *
 * [wanted] runs from the most important page to the least and may repeat pages. A page that becomes more important
 * than a running download takes its slot, while downloads that stay among the most important keep running when the
 * order changes, so turning a page doesn't restart the downloads of the pages after it.
 */
internal fun <T> schedulePageLoads(
    wanted: List<T>,
    loading: Set<T>,
    missing: (T) -> Boolean,
    slots: Int,
): PageLoadChanges<T> {
    val chosen = wanted.asSequence()
        .distinct()
        .filter { it in loading || missing(it) }
        .take(slots)
        .toSet()
    return PageLoadChanges(
        start = chosen.filter { it !in loading },
        cancel = loading.filter { it !in chosen },
    )
}
