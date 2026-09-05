package mihon.entry.interactions.book.document.reader.table

import androidx.compose.ui.text.MultiParagraph
import androidx.compose.ui.text.MultiParagraphIntrinsics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.resolveDefaults
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.ceil

/** Shares shaping between width and height passes, and between identical cell labels. */
internal class BookDocumentTableTextMeasurer(
    private val density: Density,
    private val layoutDirection: LayoutDirection,
    private val fontFamilyResolver: FontFamily.Resolver,
) {
    // Discard these caches after calculating geometry; retain no offscreen text layouts.
    private val intrinsics = mutableMapOf<BookDocumentTableCellText, MultiParagraphIntrinsics>()
    private val heights = mutableMapOf<Pair<BookDocumentTableCellText, Int>, Int>()

    fun intrinsics(text: BookDocumentTableCellText): MultiParagraphIntrinsics = intrinsics.getOrPut(text) {
        MultiParagraphIntrinsics(
            annotatedString = text.text,
            style = resolveDefaults(text.style, layoutDirection),
            placeholders = emptyList(),
            density = density,
            fontFamilyResolver = fontFamilyResolver,
            softWrap = true,
        )
    }

    fun height(text: BookDocumentTableCellText, width: Int): Int = heights.getOrPut(text to width) {
        ceil(
            MultiParagraph(
                intrinsics = intrinsics(text),
                constraints = Constraints.fitPrioritizingWidth(
                    minWidth = 0,
                    maxWidth = width,
                    minHeight = 0,
                    maxHeight = Constraints.Infinity,
                ),
            ).height,
        ).toInt()
    }
}
