package mihon.entry.interactions.book.document.reader.table

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onLayoutRectChanged
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import mihon.book.api.document.BookDocumentBlock
import mihon.book.api.document.BookDocumentTableLayout

/** Keeps a complete row grid while composing selectable cells only near the window. */
@Composable
internal fun BookDocumentTableGrid(
    grid: BookDocumentTableLayout,
    block: BookDocumentBlock,
    viewportWidth: Dp,
    modifier: Modifier = Modifier,
    content: @Composable (Int) -> Unit,
) {
    val geometry = rememberBookDocumentTableGeometry(grid, block, viewportWidth)
    val windowHeight = LocalWindowInfo.current.containerSize.height
    // Forward prefetch has no window coordinates yet; prepare only the leading overscan rows.
    var windowTop by remember { mutableIntStateOf(windowHeight) }
    // Half a screen of overscan covers movement between the parent's placement and its bounds callback.
    val visibleCells by remember(geometry, windowHeight) {
        derivedStateOf {
            geometry.cells.indices.filter { index ->
                val bounds = geometry.cells[index]
                bounds.bottom + windowTop >= -windowHeight / 2 && bounds.top + windowTop <= windowHeight * 3 / 2
            }
        }
    }
    val indices = visibleCells
    Layout(
        content = {
            indices.forEach { index -> key(index) { content(index) } }
        },
        modifier = modifier.onLayoutRectChanged(throttleMillis = 0, debounceMillis = 0) {
            windowTop = it.positionInWindow.y
        },
    ) { measurables, constraints ->
        val placeables = measurables.mapIndexed { position, measurable ->
            val index = indices[position]
            val bounds = geometry.cells[index]
            index to measurable.measure(Constraints.fixedWidth(bounds.width))
        }
        layout(constraints.constrainWidth(geometry.width), constraints.constrainHeight(geometry.height)) {
            placeables.forEach { (index, placeable) ->
                val bounds = geometry.cells[index]
                placeable.placeRelative(bounds.left, bounds.top)
            }
        }
    }
}
