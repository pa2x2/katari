package mihon.presentation.kit.content

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Fixed geometry of an entry's details screen that the spacing scale has no step for. */
object EntryMetrics {

    /** Widest the portrait cover gets beside the title in the phone header. */
    val headerCoverMaxWidth: Dp = 132.dp

    /** The same for landscape covers, such as video thumbnails. */
    val headerWideCoverMaxWidth: Dp = 160.dp

    /** Preferred width of a page in the preview grid at each preview size; the grid fits as many as the width allows. */
    val previewTileSmall: Dp = 88.dp
    val previewTileMedium: Dp = 112.dp
    val previewTileLarge: Dp = 140.dp
    val previewTileExtraLarge: Dp = 272.dp

    /** Tallest the member list of the merge editor dialog gets before it scrolls. */
    val mergeEditorListMaxHeight: Dp = 360.dp

    /** Width of a candidate's cover in the possible-duplicates sheet. */
    val duplicateCoverWidth: Dp = 96.dp

    /** Height of a result's cover in the tracker search. */
    val trackerResultCoverHeight: Dp = 96.dp
}
