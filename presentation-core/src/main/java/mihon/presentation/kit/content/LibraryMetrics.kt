package mihon.presentation.kit.content

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/** Fixed geometry of entry covers in library and browse grids and lists that the spacing scale has no step for. */
object LibraryMetrics {
    /**
     * The cover of a row led by an entry, such as in history, updates or a list display mode: one portrait shape, so
     * posters and book covers line up.
     */
    val rowCover: DpSize = DpSize(40.dp, 56.dp)

    /** Preferred width of a list row in the horizontally scrolling shelf of pinned entries. */
    val pinnedShelfRowWidth: Dp = 280.dp

    /** Tallest the reorderable list of the library grouping editor gets before it scrolls. */
    val groupingEditorMaxHeight: Dp = 280.dp
}
