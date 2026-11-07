package mihon.presentation.kit.content

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Geometry of browse surfaces (sources, extensions, catalogs and feeds) that the spacing scale has no step for, with
 * entry covers at phone size. Wider windows scale covers with [entryCoverScale].
 */
object BrowseMetrics {
    /** Width of a cover in a horizontally scrolling row of search results. */
    val searchResultCoverWidth: Dp = 96.dp

    /** Height of an entry's portrait cover in a list row that is laid out around it, such as a reading session's. */
    val listCoverHeight: Dp = 80.dp

    /** Widest an entry's cover gets in the side-by-side list of source migration, current entry beside its match. */
    val migrationCoverMaxWidth: Dp = 150.dp
}
