package mihon.entry.interactions.book.document.reader.box

import mihon.book.api.document.BookDocumentBox
import mihon.book.api.document.BookDocumentBoxStyle
import mihon.book.api.document.BookDocumentContent

/**
 * One block's share of a box. A box spanning several blocks is drawn as one slice per row or page fragment; only the
 * slice that [opens] the box draws its top edge, and only the one that [closes] it its bottom edge.
 */
internal data class BookDocumentBoxSlice(
    val style: BookDocumentBoxStyle,
    val opens: Boolean,
    val closes: Boolean,
)

/** The boxes around each block of one content, outermost first, so a row finds its own without scanning them all. */
internal class BookDocumentBoxIndex(blockCount: Int, boxes: List<BookDocumentBox>) {
    private val around: Array<List<BookDocumentBox>> = Array(blockCount) { emptyList() }

    constructor(content: BookDocumentContent) : this(content.blocks.size, content.boxes)

    init {
        boxes.forEach { box ->
            for (index in box.firstBlockIndex..box.lastBlockIndex) around[index] = around[index] + box
        }
    }

    /**
     * Slices around the block at [blockIndex]. [startsBlock] and [endsBlock] are false for a page fragment that
     * continues the block from the previous page or on the next one, which then leaves those edges open.
     */
    fun slices(blockIndex: Int, startsBlock: Boolean = true, endsBlock: Boolean = true): List<BookDocumentBoxSlice> =
        around.getOrNull(blockIndex).orEmpty().map { box ->
            BookDocumentBoxSlice(
                style = box.style,
                opens = startsBlock && box.firstBlockIndex == blockIndex,
                closes = endsBlock && box.lastBlockIndex == blockIndex,
            )
        }

    /** How many of the innermost boxes around the block at [blockIndex] hold that block alone. */
    fun ownBoxCount(blockIndex: Int): Int = around.getOrNull(blockIndex).orEmpty()
        .count { it.firstBlockIndex == blockIndex && it.lastBlockIndex == blockIndex }
}
