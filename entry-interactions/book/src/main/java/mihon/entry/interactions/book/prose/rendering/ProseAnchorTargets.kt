package mihon.entry.interactions.book.prose

import mihon.book.api.document.BookDocumentBlockContent
import mihon.entry.interactions.book.document.render.PreparedBookDocumentBlock

internal data class ProseDisclosureAnchorTarget(
    val block: PreparedBookDocumentBlock,
    val offsetWithinBlock: Int,
)

internal fun resolveProseDisclosureAnchorTarget(
    semantic: BookDocumentBlockContent.Disclosure,
    body: List<PreparedBookDocumentBlock>,
    offsetWithinDisclosure: Int,
): ProseDisclosureAnchorTarget? {
    val bodyOffset = offsetWithinDisclosure - semantic.bodyStartWithinBlock
    if (bodyOffset < 0) return null
    val block = body.firstOrNull { candidate ->
        bodyOffset >= candidate.block.logicalStart &&
            (
                bodyOffset < candidate.block.logicalEndExclusive ||
                    (
                        candidate == body.last() &&
                            bodyOffset == candidate.block.logicalEndExclusive
                        )
                )
    } ?: return null
    return ProseDisclosureAnchorTarget(
        block = block,
        offsetWithinBlock = (bodyOffset - block.block.logicalStart)
            .coerceIn(0, block.block.logicalLength),
    )
}

internal sealed interface ProseTableAnchorTarget {
    data class Caption(val characterOffset: Int) : ProseTableAnchorTarget
    data class Cell(
        val rowIndex: Int,
        val cellIndex: Int,
        val characterOffset: Int,
    ) : ProseTableAnchorTarget
}

internal fun BookDocumentBlockContent.Table.resolveProseTableAnchorTarget(
    offsetWithinBlock: Int,
): ProseTableAnchorTarget {
    val target = offsetWithinBlock.coerceAtLeast(0)
    var logicalOffset = 0
    caption?.let { value ->
        if (target <= value.range.endExclusive) {
            return ProseTableAnchorTarget.Caption(
                (target - value.range.start).coerceIn(0, value.text.length),
            )
        }
        logicalOffset = value.range.endExclusive + 1
    }
    rows.forEachIndexed { rowIndex, row ->
        val cells = row.cells.mapIndexed { cellIndex, cell ->
            logicalOffset = cell.content.range.endExclusive
            Triple(cellIndex, cell.content.range.start, cell.content.range.endExclusive)
        }
        if (target <= logicalOffset) {
            val selected = cells.lastOrNull { (_, start) -> target >= start } ?: cells.first()
            return ProseTableAnchorTarget.Cell(
                rowIndex = rowIndex,
                cellIndex = selected.first,
                characterOffset = (target - selected.second).coerceIn(0, selected.third - selected.second),
            )
        }
        logicalOffset++
    }
    val lastRowIndex = rows.lastIndex
    val lastCellIndex = rows.last().cells.lastIndex
    return ProseTableAnchorTarget.Cell(
        rowIndex = lastRowIndex,
        cellIndex = lastCellIndex,
        characterOffset = rows.last().cells.last().text.length,
    )
}
