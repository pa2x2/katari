package mihon.presentation.kit.content

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

/**
 * Fixed geometry of a reading page that the spacing scale has no step for, for content renderers such as the book and
 * manga readers. Paginated readers measure text off-screen with the same values they draw with, so both read them here.
 */
object ReadingMetrics {
    /** Side inset of every block of text on the page. */
    val blockInset: Dp = 20.dp

    /** Side inset of every block of text with the reader's chosen [margin]. */
    fun blockInset(margin: ReadingMargin): Dp = when (margin) {
        ReadingMargin.Standard -> blockInset
        ReadingMargin.Narrow -> 8.dp
        ReadingMargin.Normal -> 28.dp
        ReadingMargin.Wide -> 48.dp
    }

    /**
     * Widest a column of running text grows on a wide window: the longest line that stays easy to follow at [fontSize]
     * (about 70 characters, so it grows with the text) plus [inset] on each side.
     */
    fun columnWidth(fontSize: TextUnit, inset: Dp, density: Density): Dp =
        with(density) { (fontSize * LINE_LENGTH_EM).toDp() } + inset * 2

    /** Space above and below every block of text on the page. */
    val blockGap: Dp = 6.dp

    /** Width of the rule beside a quotation. */
    val quoteRuleWidth: Dp = 3.dp

    /** Space above and below the boundary between two chapters of a scrolled document. */
    val chapterBoundaryGap: Dp = 28.dp

    /** Space above and below the boundary between two chapters of a continuous image strip, such as a webtoon. */
    val stripChapterBoundaryGap: Dp = 128.dp

    /** That space while only a progress indicator stands in for the boundary, before the next chapter has loaded. */
    val stripChapterLoadingGap: Dp = 64.dp

    /** Width of the filled part of a progress rail along the page edge. */
    val progressFillWidth: Dp = 2.dp

    /** Width of the position marker on a progress rail. */
    val progressMarkerWidth: Dp = 4.dp

    /** Length of the position marker on a progress rail. */
    val progressMarkerLength: Dp = 32.dp
}

/** A comfortable line of running text, in multiples of its font size. */
private const val LINE_LENGTH_EM = 32.5f

/** Side margins a reader can pick for its text; [Standard] is the page's own [ReadingMetrics.blockInset]. */
enum class ReadingMargin { Standard, Narrow, Normal, Wide }
