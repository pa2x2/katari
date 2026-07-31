package mihon.entry.interactions.book.prose

import android.graphics.Typeface
import android.text.Layout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentInlineStyleRange
import mihon.book.api.document.BookDocumentLink
import mihon.book.api.document.BookDocumentTableCellScope
import mihon.book.api.document.layoutBookDocumentTable

@Composable
internal fun ProseTable(
    semantic: BookDocumentBlockContent.Table,
    documentTextIdentityPrefix: String,
    foreground: Color,
    background: Color,
    readerTypeface: Typeface,
    readerTextSizeSp: Float,
    lineSpacingMultiplier: Float,
    onAnchorClick: (String, TextView) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    inlineStyles: List<BookDocumentInlineStyleRange>,
    inlineTypefaces: Map<String, Typeface>,
    anchorOffsetWithinBlock: Int?,
    onAnchorTargetPositioned: (LayoutCoordinates, Int) -> Unit,
    modifier: Modifier,
) {
    val rows = remember(semantic) { semantic.toDisplayRows() }
    val anchorTarget = remember(semantic, anchorOffsetWithinBlock) {
        anchorOffsetWithinBlock?.let { semantic.resolveProseTableAnchorTarget(it) }
    }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        semantic.caption?.let { caption ->
            ProseRichText(
                text = caption.text.toSpanned(
                    links = caption.links,
                    inlineStyles = caption.inlineStyles,
                    inlineTypefaces = inlineTypefaces,
                ),
                documentTextIdentity = buildString {
                    append(documentTextIdentityPrefix)
                    append(":caption:")
                    append(inlineStyles.hashCode())
                    append(':')
                    append(inlineTypefaces.keys.sorted().joinToString())
                },
                textColor = foreground.toArgbValue(),
                textSizeSp = readerTextSizeSp,
                typeface = Typeface.create(readerTypeface, Typeface.BOLD),
                lineSpacingMultiplier = lineSpacingMultiplier,
                textAlignment = TextView.TEXT_ALIGNMENT_VIEW_START,
                justificationMode = Layout.JUSTIFICATION_MODE_NONE,
                onAnchorClick = onAnchorClick,
                onExternalLinkClick = onExternalLinkClick,
                anchorCharacterOffset = (anchorTarget as? ProseTableAnchorTarget.Caption)?.characterOffset,
                onAnchorTargetPositioned = onAnchorTargetPositioned,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .border(1.dp, foreground.copy(alpha = 0.35f)),
        ) {
            rows.forEachIndexed { rowIndex, row ->
                Row {
                    row.cells.forEach { cell ->
                        val cellBackground = if (cell.header) {
                            foreground.copy(alpha = 0.12f).compositeOver(background)
                        } else {
                            background
                        }
                        val cellTarget = (anchorTarget as? ProseTableAnchorTarget.Cell)
                            ?.takeIf {
                                it.rowIndex == rowIndex &&
                                    it.cellIndex == cell.sourceCellIndex
                            }
                        ProseRichText(
                            text = cell.displayText.toSpanned(
                                links = cell.links,
                                inlineStyles = inlineStyles.within(
                                    cell.logicalStart,
                                    cell.logicalEndExclusive,
                                ),
                                inlineTypefaces = inlineTypefaces,
                            ),
                            documentTextIdentity = buildString {
                                append(documentTextIdentityPrefix)
                                append(':')
                                append(cell.logicalStart)
                                append(':')
                                append(cell.logicalEndExclusive)
                                append(':')
                                append(inlineStyles.hashCode())
                                append(':')
                                append(inlineTypefaces.keys.sorted().joinToString())
                            },
                            modifier = Modifier
                                .width(TABLE_COLUMN_WIDTH * cell.columnSpan)
                                .background(cellBackground)
                                .border(0.5.dp, foreground.copy(alpha = 0.25f))
                                .then(
                                    if (cell.header) {
                                        Modifier.semantics {
                                            heading()
                                            contentDescription = when (cell.scope) {
                                                BookDocumentTableCellScope.ROW -> "Row header: ${cell.displayText}"
                                                BookDocumentTableCellScope.COLUMN ->
                                                    "Column header: ${cell.displayText}"
                                                BookDocumentTableCellScope.ROW_GROUP ->
                                                    "Row group header: ${cell.displayText}"
                                                BookDocumentTableCellScope.COLUMN_GROUP ->
                                                    "Column group header: ${cell.displayText}"
                                                null -> "Table header: ${cell.displayText}"
                                            }
                                        }
                                    } else {
                                        Modifier
                                    },
                                )
                                .padding(8.dp),
                            textColor = foreground.toArgbValue(),
                            textSizeSp = readerTextSizeSp * TABLE_TEXT_SCALE,
                            typeface = if (cell.header) {
                                Typeface.create(readerTypeface, Typeface.BOLD)
                            } else {
                                readerTypeface
                            },
                            lineSpacingMultiplier = lineSpacingMultiplier,
                            textAlignment = TextView.TEXT_ALIGNMENT_VIEW_START,
                            justificationMode = Layout.JUSTIFICATION_MODE_NONE,
                            onAnchorClick = onAnchorClick,
                            onExternalLinkClick = onExternalLinkClick,
                            anchorCharacterOffset = cellTarget?.characterOffset,
                            onAnchorTargetPositioned = onAnchorTargetPositioned,
                        )
                    }
                }
                if (rowIndex != rows.lastIndex) {
                    HorizontalDivider(color = foreground.copy(alpha = 0.2f))
                }
            }
        }
    }
}

internal fun BookDocumentBlockContent.Table.toDisplayRows(): List<DisplayTableRow> {
    val layout = checkNotNull(rows.layoutBookDocumentTable())
    check(layout.columnCount == columnCount)
    return layout.rows.map { row ->
        val placements = row.placements.mapIndexed { cellIndex, placement ->
            placement.column to (placement to cellIndex)
        }.toMap()
        val displayed = mutableListOf<DisplayTableCell>()
        var column = 0
        while (column < columnCount) {
            val placed = placements[column]
            if (placed != null) {
                val (placement, sourceCellIndex) = placed
                val cell = placement.cell
                displayed += DisplayTableCell(
                    displayText = cell.text.ifEmpty { " " },
                    header = cell.header,
                    scope = cell.scope,
                    columnSpan = cell.columnSpan,
                    links = cell.links,
                    sourceCellIndex = sourceCellIndex,
                    logicalStart = cell.content.range.start,
                    logicalEndExclusive = cell.content.range.endExclusive,
                )
                column += cell.columnSpan
                continue
            }
            displayed += DisplayTableCell(
                displayText = if (column in row.carriedColumns) "↳" else " ",
                header = false,
                scope = null,
                columnSpan = 1,
                links = emptyList(),
                sourceCellIndex = -1,
                logicalStart = 0,
                logicalEndExclusive = 0,
            )
            column++
        }
        DisplayTableRow(displayed)
    }
}

internal data class DisplayTableRow(
    val cells: List<DisplayTableCell>,
)

internal data class DisplayTableCell(
    val displayText: String,
    val header: Boolean,
    val scope: BookDocumentTableCellScope?,
    val columnSpan: Int,
    val links: List<BookDocumentLink>,
    val sourceCellIndex: Int,
    val logicalStart: Int,
    val logicalEndExclusive: Int,
)

internal val TABLE_COLUMN_WIDTH = 150.dp
internal const val TABLE_TEXT_SCALE = 0.875f
