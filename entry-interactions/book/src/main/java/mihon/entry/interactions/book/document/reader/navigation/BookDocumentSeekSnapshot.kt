package mihon.entry.interactions.book.document.reader.navigation

import mihon.book.api.document.BookDocumentPosition
import mihon.book.api.document.locatorAt
import mihon.entry.interactions.book.document.reader.BookDocumentSection
import mihon.entry.interactions.book.document.reader.paging.BookDocumentPage
import tachiyomi.domain.entry.model.EntryChapter
import kotlin.math.roundToInt

/** Resolves displayed navigation units to canonical passages within one document section. */
internal data class BookDocumentSeekSnapshot(
    val section: BookDocumentSection<EntryChapter>,
    val position: BookDocumentPosition,
    val pagePositions: List<BookDocumentPosition>,
    val pages: List<BookDocumentPage> = emptyList(),
    val viewportEndProgression: Float? = null,
) {
    val paged: Boolean get() = pagePositions.isNotEmpty()
    val range: ClosedFloatingPointRange<Float> get() = if (paged) 1f..pagePositions.size.toFloat() else 0f..100f
    val value: Float get() = if (paged) {
        val offset = section.document.document.logicalOffset(position) ?: 0
        (pagePositions.indexOfLast { (section.document.document.logicalOffset(it) ?: 0) <= offset } + 1)
            .coerceAtLeast(1).toFloat()
    } else {
        val start = section.document.document.progressionAt(position)
        when {
            start == 0f -> 0f
            viewportEndProgression == 1f -> 100f
            else -> (start / scrollableExtent * 100f).coerceIn(0f, 100f)
        }
    }

    private val scrollableExtent: Float get() =
        (
            1f - (
                (viewportEndProgression ?: section.document.document.progressionAt(position)) -
                    section.document.document.progressionAt(position)
                )
            ).coerceIn(.0001f, 1f)

    fun positionAt(value: Float): BookDocumentPosition = if (paged) {
        pagePositions[(value.roundToInt() - 1).coerceIn(pagePositions.indices)]
    } else {
        section.document.document.positionAtProgression(if (value >= 100f) 1f else value / 100f * scrollableExtent)
    }

    fun targetAt(value: Float): BookDocumentNavigationTarget = BookDocumentNavigationTarget(
        chapter = section.owner,
        locator = section.document.document.locatorAt(positionAt(value)),
        restorePosition = true,
    )

    fun previewAt(value: Float): String {
        val document = section.document.document
        val offset = document.logicalOffset(positionAt(value)) ?: 0
        val text = document.content.text
        val start = offset.coerceAtMost((text.length - 120).coerceAtLeast(0))
        return text.substring(start, (start + 200).coerceAtMost(text.length)).replace(WHITESPACE, " ").trim()
    }
}

private val WHITESPACE = Regex("\\s+")
