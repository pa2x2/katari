package mihon.presentation.kit.content

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Fixed geometry of browse surfaces (sources, extensions, catalogs and feeds) that the spacing scale has no step for. */
object BrowseMetrics {
    /** Width of a cover in a horizontally scrolling row of search results. */
    val searchResultCoverWidth: Dp = 96.dp

    /** Height of an entry's portrait cover in a list row that is laid out around it, such as migration's. */
    val listCoverHeight: Dp = 80.dp

    /** Width such a row keeps for the cover, so portrait and landscape covers line up. */
    val listCoverSlotWidth: Dp = 72.dp

    /** Shortest such a row gets, so rows with one or several lines of details line up. */
    val listCoverRowMinHeight: Dp = 96.dp

    /** Widest an entry's cover gets in the side-by-side list of source migration, current entry beside its match. */
    val migrationCoverMaxWidth: Dp = 150.dp
}
