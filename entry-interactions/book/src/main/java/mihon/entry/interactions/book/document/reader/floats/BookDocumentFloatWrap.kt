package mihon.entry.interactions.book.document.reader.floats

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import mihon.book.api.document.BookDocumentBlock
import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentBlockKind
import mihon.book.api.document.BookDocumentContent
import mihon.book.api.document.BookDocumentFloat
import mihon.book.api.document.BookDocumentLinkTarget
import mihon.entry.interactions.book.document.reader.BOOK_DOCUMENT_BASE_TEXT_SIZE_SP
import mihon.entry.interactions.book.document.reader.BookDocumentBlockRenderer
import mihon.entry.interactions.book.document.reader.LocalBookDocumentTypography
import mihon.entry.interactions.book.document.reader.box.BookDocumentBoxSlice
import mihon.entry.interactions.book.document.reader.box.bookDocumentBoxes
import mihon.entry.interactions.book.document.reader.measureBookDocumentText
import mihon.entry.interactions.book.document.reader.paging.pageTextSlice
import mihon.entry.interactions.book.document.reader.rememberBookDocumentFonts
import mihon.entry.interactions.book.document.reader.theme.LocalBookDocumentAuthoredStyling
import mihon.entry.interactions.book.preparation.BookPublicationResourceLoader
import mihon.presentation.kit.theme.KatariTheme

/** Whether [this] block is text the float before it can wrap around. */
internal fun BookDocumentBlock.wrapsAroundFloat(): Boolean =
    content is BookDocumentBlockContent.Text && !style.flow.clearsFloats

/**
 * Draws a floated block beside the start of the text block after it. The text is measured at the width left beside
 * the float and split at the first line below it, so the rest runs at full width; the split is a text offset, so
 * both parts keep the block's reading positions.
 */
@Composable
internal fun BookDocumentFloatWrap(
    float: BookDocumentBlock,
    floatSlices: List<BookDocumentBoxSlice>,
    text: BookDocumentBlock,
    owningContent: BookDocumentContent,
    sectionKey: String,
    resourceLoader: BookPublicationResourceLoader?,
    onAnchorClick: (BookDocumentLinkTarget) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    preserveTerminalSpacing: Boolean,
    modifier: Modifier = Modifier,
) {
    val spec = checkNotNull(float.style.flow.float)
    val styling = LocalBookDocumentAuthoredStyling.current
    val typography = LocalBookDocumentTypography.current
    val emDp = BOOK_DOCUMENT_BASE_TEXT_SIZE_SP * typography.textScale
    val fonts = rememberBookDocumentFonts(text.style.fontFamily, text.inlineStyles)
    val textMeasurer = rememberTextMeasurer(cacheSize = 0)
    val gap = KatariTheme.spacing.m
    val quoteInset = if (text.role.kind == BookDocumentBlockKind.QUOTE) KatariTheme.spacing.m else 0.dp

    @Composable
    fun Part(block: BookDocumentBlock, identity: String, keepTerminalSpacing: Boolean) = BookDocumentBlockRenderer(
        block = block,
        owningContent = owningContent,
        sectionKey = sectionKey,
        resourceLoader = resourceLoader,
        onAnchorClick = onAnchorClick,
        onExternalLinkClick = onExternalLinkClick,
        selectionIdentity = identity,
        preserveTerminalSpacing = keepTerminalSpacing,
    )

    SubcomposeLayout(modifier) { constraints ->
        val available = constraints.maxWidth
        val emPx = emDp.dp.toPx()
        val gapPx = gap.roundToPx()
        val floatWidth = spec.widthPx(available, emPx)
        val floatPlaceable = subcompose("float") {
            BookDocumentBlockRenderer(
                block = float,
                owningContent = owningContent,
                sectionKey = sectionKey,
                resourceLoader = resourceLoader,
                onAnchorClick = onAnchorClick,
                onExternalLinkClick = onExternalLinkClick,
                modifier = Modifier.bookDocumentBoxes(floatSlices, styling, emDp),
            )
        }.single().measure(Constraints(minWidth = floatWidth, maxWidth = floatWidth))
        val besideWidth = (available - floatWidth - gapPx).coerceAtLeast(1)
        val source = owningContent.text
        val measured = textMeasurer.measureBookDocumentText(
            block = text,
            blockText = source.substring(text.logicalStart, text.logicalEndExclusive),
            fonts = fonts,
            typography = typography,
            width = besideWidth - quoteInset.roundToPx(),
        )
        val spacingBefore = text.style.spacingBeforeEm * emPx
        val lines = measured.layout.lineCount
        val besideLines = (0 until lines).count { line ->
            measured.layout.getLineTop(line) + spacingBefore < floatPlaceable.height
        }
        val split = if (besideLines in 1 until lines) measured.sourceLineStart(besideLines) else null
        val (beside, below) = if (split != null && split in 1 until text.logicalLength) {
            val first = text.pageTextSlice(source, 0, split)
            val rest = text.pageTextSlice(source, split, text.logicalLength)
            first.withoutSpacingAfter() to rest.withoutSpacingBefore()
        } else {
            text to null
        }
        val besidePlaceable = subcompose("beside") {
            Part(beside, text.id.value, keepTerminalSpacing = below == null && preserveTerminalSpacing)
        }.single().measure(Constraints(minWidth = besideWidth, maxWidth = besideWidth))
        val belowPlaceable = below?.let { block ->
            subcompose("below") {
                Part(block, "${text.id.value}:below-float", keepTerminalSpacing = preserveTerminalSpacing)
            }.single().measure(Constraints(minWidth = available, maxWidth = available))
        }
        val besideHeight = maxOf(floatPlaceable.height, besidePlaceable.height)
        layout(available, besideHeight + (belowPlaceable?.height ?: 0)) {
            val left = spec.side == BookDocumentFloat.Side.LEFT
            floatPlaceable.place(if (left) 0 else available - floatWidth, 0)
            besidePlaceable.place(if (left) floatWidth + gapPx else 0, 0)
            belowPlaceable?.place(0, besideHeight)
        }
    }
}

private fun BookDocumentBlock.withoutSpacingAfter() = copy(
    style = style.copy(flow = style.flow.copy(spacingAfterEm = 0f)),
)

private fun BookDocumentBlock.withoutSpacingBefore() =
    copy(style = style.copy(flow = style.flow.copy(spacingBeforeEm = 0f, firstLineIndentEm = 0f)))
