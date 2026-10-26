package mihon.book.api.document

/**
 * Replaces [start] up to [endExclusive] of a text block's canonical text with [replacement]; an empty replacement
 * removes the range.
 */
data class BookDocumentTextEdit(
    val start: Int,
    val endExclusive: Int,
    val replacement: String,
) {
    init {
        require(start >= 0) { "document text edit start must not be negative" }
        require(endExclusive >= start) { "document text edit end must not precede its start" }
    }
}

/**
 * This document with the top-level text blocks changed by the [edits] each is given, as a reader's own rules for hiding
 * or replacing text do. [edits] receives a block and its canonical text and returns ordered, non-overlapping edits
 * of that text. Links and styles keep to the text they covered, a block whose text is left blank is removed together
 * with the gap before it, and anchors, boxes and later blocks follow. Other kinds of block are left as they are.
 */
fun BookDocument.withTextBlockEdits(edits: (BookDocumentBlock, String) -> List<BookDocumentTextEdit>): BookDocument {
    val source = content
    val text = StringBuilder(source.text.length)
    val blocks = mutableListOf<BookDocumentBlock>()
    // Old block index to new, or null for a removed block; each kept block's edits map its anchor offsets.
    val newIndices = arrayOfNulls<Int>(source.blocks.size)
    val blockEdits = mutableMapOf<BookDocumentBlockId, BookDocumentTextEditMap>()
    var changed = false
    var copiedUpTo = 0
    source.blocks.forEachIndexed { index, block ->
        // Before the first block that stays, the document's own leading text rather than a removed block's gap.
        val gap = if (blocks.isEmpty()) {
            source.text.substring(0, source.blocks.first().logicalStart)
        } else {
            source.text.substring(copiedUpTo, block.logicalStart)
        }
        copiedUpTo = block.logicalEndExclusive
        val blockText = source.text.substring(block.logicalStart, block.logicalEndExclusive)
        val textContent = block.content as? BookDocumentBlockContent.Text
        val map = textContent?.let { BookDocumentTextEditMap(edits(block, blockText), blockText.length) }
            ?.takeIf { it.changes }
        if (map == null) {
            text.append(gap)
            newIndices[index] = blocks.size
            blocks += block.movedTo(text.length)
            text.append(blockText)
            return@forEachIndexed
        }
        changed = true
        val edited = map.apply(blockText)
        if (edited.isBlank()) return@forEachIndexed
        text.append(gap)
        newIndices[index] = blocks.size
        blockEdits[block.id] = map
        blocks += block.edited(textContent, edited, map, text.length)
        text.append(edited)
    }
    if (!changed) return this
    if (blocks.isEmpty()) return this
    text.appendRange(source.text, copiedUpTo, source.text.length)
    return copy(
        content = BookDocumentContent(
            text = text.toString(),
            blocks = blocks,
            anchors = source.anchors.mapValues { (_, position) ->
                position.followingEdits(source.blocks, newIndices, blocks, blockEdits)
            },
            resourceIds = blocks.flatMapTo(linkedSetOf()) { it.referencedResourceIds() },
            boxes = source.boxes.mapNotNull { box ->
                val first = (box.firstBlockIndex..box.lastBlockIndex).firstNotNullOfOrNull { newIndices[it] }
                val last = (box.lastBlockIndex downTo box.firstBlockIndex).firstNotNullOfOrNull { newIndices[it] }
                if (first == null || last == null) null else box.copy(firstBlockIndex = first, lastBlockIndex = last)
            },
        ),
    )
}

private fun BookDocumentBlock.movedTo(start: Int): BookDocumentBlock =
    copy(logicalStart = start, logicalEndExclusive = start + logicalLength)

private fun BookDocumentBlock.edited(
    textContent: BookDocumentBlockContent.Text,
    edited: String,
    map: BookDocumentTextEditMap,
    start: Int,
): BookDocumentBlock {
    val value = textContent.value
    return copy(
        content = textContent.copy(
            value = BookDocumentRichText(
                text = edited,
                range = BookDocumentTextRange(0, edited.length),
                links = value.links.mapNotNull { link ->
                    map.range(link.start, link.endExclusive)?.let { (from, to) ->
                        link.copy(start = from, endExclusive = to)
                    }
                },
                inlineStyles = value.inlineStyles.mapNotNull { style ->
                    map.range(style.start, style.endExclusive)?.let { (from, to) ->
                        style.copy(start = from, endExclusive = to)
                    }
                },
            ),
        ),
        plainText = edited.trim(),
        logicalStart = start,
        logicalEndExclusive = start + edited.length,
    )
}

/** An anchor in a removed block moves to the start of the next block that stays, or the end of the one before. */
private fun BookDocumentPosition.followingEdits(
    oldBlocks: List<BookDocumentBlock>,
    newIndices: Array<Int?>,
    newBlocks: List<BookDocumentBlock>,
    blockEdits: Map<BookDocumentBlockId, BookDocumentTextEditMap>,
): BookDocumentPosition {
    val oldIndex = oldBlocks.indexOfFirst { it.id == blockId }
    newIndices.getOrNull(oldIndex)?.let { kept ->
        val offset = blockEdits[blockId]?.start(offsetWithinBlock) ?: offsetWithinBlock
        return BookDocumentPosition(blockId, offset.coerceIn(0, newBlocks[kept].logicalLength))
    }
    (oldIndex + 1 until newIndices.size).firstNotNullOfOrNull { newIndices[it] }?.let { next ->
        return BookDocumentPosition(newBlocks[next].id, 0)
    }
    val previous = newBlocks.last()
    return BookDocumentPosition(previous.id, previous.logicalLength)
}

/** Maps offsets of a block's text through its edits. */
private class BookDocumentTextEditMap(edits: List<BookDocumentTextEdit>, length: Int) {
    // Edits that change nothing are dropped, so a block they leave alone stays untouched.
    private val edits = edits.filter { edit ->
        edit.endExclusive <= length && (edit.endExclusive > edit.start || edit.replacement.isNotEmpty())
    }

    init {
        require(this.edits.zipWithNext().all { (first, second) -> first.endExclusive <= second.start }) {
            "document text edits must be ordered and must not overlap"
        }
    }

    val changes: Boolean get() = edits.isNotEmpty()

    fun apply(text: String): String = buildString(text.length) {
        var copied = 0
        edits.forEach { edit ->
            appendRange(text, copied, edit.start)
            append(edit.replacement)
            copied = edit.endExclusive
        }
        appendRange(text, copied, text.length)
    }

    /** Where [offset] lands; inside an edited range, at the start of what replaced it. */
    fun start(offset: Int): Int = map(offset, toEnd = false)

    /** The new range of [start] up to [endExclusive]; null when nothing of it is left. */
    fun range(start: Int, endExclusive: Int): Pair<Int, Int>? {
        val from = map(start, toEnd = false)
        val to = map(endExclusive, toEnd = true)
        return (from to to).takeIf { to > from }
    }

    private fun map(offset: Int, toEnd: Boolean): Int {
        var shift = 0
        for (edit in edits) {
            if (offset <= edit.start) break
            if (offset < edit.endExclusive) {
                return edit.start + shift + if (toEnd) edit.replacement.length else 0
            }
            shift += edit.replacement.length - (edit.endExclusive - edit.start)
        }
        return offset + shift
    }
}
