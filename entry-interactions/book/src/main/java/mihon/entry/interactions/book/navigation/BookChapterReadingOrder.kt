package mihon.entry.interactions.book.navigation

import mihon.entry.interactions.viewer.EntryChildWindow
import tachiyomi.domain.entry.model.EntryChapter

/**
 * Stable reading order for one BOOK entry, with constant-time chapter positioning.
 *
 * The document reader observes locations on the main thread, so finding an adjacent chapter or
 * activating a chapter must not repeatedly walk a large catalogue.
 */
internal class BookChapterReadingOrder(chapters: List<EntryChapter>) {
    val chapters = chapters.toList()

    private val indicesByChapterId = buildMap(chapters.size) {
        chapters.forEachIndexed { index, chapter ->
            check(put(chapter.id, index) == null) { "BOOK reading order contains duplicate chapter ${chapter.id}" }
        }
    }

    fun indexOf(chapterId: Long): Int = indicesByChapterId[chapterId] ?: -1

    fun window(chapterId: Long): EntryChildWindow<EntryChapter>? {
        val index = indexOf(chapterId)
        if (index !in chapters.indices) return null
        return EntryChildWindow(
            current = chapters[index],
            previous = chapters.getOrNull(index - 1),
            next = chapters.getOrNull(index + 1),
        )
    }
}
