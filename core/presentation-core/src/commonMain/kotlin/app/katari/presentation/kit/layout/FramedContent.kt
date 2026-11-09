package app.katari.presentation.kit.layout

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp

/**
 * A screen's content that isn't one of the kit's lists, grids or panes, such as a page laid out as one column, the
 * tabs above a pager or a button floating over a list, placed in the screen's frame: clear of the system bars, camera
 * cutout and side dock, and lined up with the title. The kit's layouts inside it don't keep clear of that room again.
 */
@Composable
fun FramedContent(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    val sides = LocalContentSides.current
    Box(modifier = modifier.sidesPadding(sides), contentAlignment = contentAlignment) {
        SidesApplied { content() }
    }
}

/**
 * A scrolling column of a screen's content that isn't rows, such as an entry's details beside its chapters. Its
 * content sits in the screen's frame like [FramedContent]'s while the scroll reaches the edges, so a drag beside the
 * content still scrolls it and artwork in it can bleed into the side room by [FramedColumnScope.columnInset].
 * [contentPadding] adds to that room, such as the room above the navigation bar.
 */
@Composable
fun FramedScrollColumn(
    modifier: Modifier = Modifier,
    state: ScrollState = rememberScrollState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable FramedColumnScope.() -> Unit,
) {
    val direction = LocalLayoutDirection.current
    val sides = LocalContentSides.current
    val padding = contentPadding.plusSides(sides, direction)
    val inset =
        PaddingValues(start = padding.calculateStartPadding(direction), end = padding.calculateEndPadding(direction))
    SidesApplied {
        Column(
            modifier = modifier
                .verticalScroll(state)
                .padding(padding)
                .consumeWindowInsets(sides.insetPadding),
        ) {
            FramedColumnScopeImpl(this, inset).content()
        }
    }
}

/** The content of a [FramedScrollColumn]. */
interface FramedColumnScope : ColumnScope {
    /**
     * How far the content sits in from the column's sides, so artwork behind it, such as an `ArtworkBackdrop`, can
     * bleed out to the column's edges.
     */
    val columnInset: PaddingValues
}

private class FramedColumnScopeImpl(
    scope: ColumnScope,
    override val columnInset: PaddingValues,
) : FramedColumnScope, ColumnScope by scope
