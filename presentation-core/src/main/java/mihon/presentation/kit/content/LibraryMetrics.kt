package mihon.presentation.kit.content

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/** Fixed geometry of entry covers in library and browse grids and lists that the spacing scale has no step for. */
object LibraryMetrics {
    /** Height of an entry's row in a cover list; the cover fills it less the row's vertical padding. */
    val listRowHeight: Dp = 56.dp

    /** The cover of a history or updates row: one portrait shape, so posters and book covers line up. */
    val rowCover: DpSize = DpSize(40.dp, 56.dp)

    /** Side of the resume button on a cover with a title over it, and in a list row. */
    val resumeButtonSmall: Dp = 28.dp

    /** Side of the resume button on a cover without a title over it. */
    val resumeButtonLarge: Dp = 36.dp

    /** Icons of the two resume button sizes. */
    val resumeIconSmall: Dp = 16.dp
    val resumeIconLarge: Dp = 22.dp

    /** Width of the outline of a resume button that fills with the entry's progress. */
    val resumeProgressStroke: Dp = 2.dp

    /** Preferred width of a list row in the horizontally scrolling shelf of pinned entries. */
    val pinnedShelfRowWidth: Dp = 280.dp

    /** Tallest the reorderable list of the library grouping editor gets before it scrolls. */
    val groupingEditorMaxHeight: Dp = 280.dp
}
