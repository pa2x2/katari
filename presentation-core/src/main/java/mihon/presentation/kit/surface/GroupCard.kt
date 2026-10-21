package mihon.presentation.kit.surface

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import mihon.presentation.kit.theme.KatariTheme

/**
 * A card that holds a group of items, such as a shelf of pinned entries or a session's chapters; it matches the cards
 * of row groups. A [raised] card floats over the content beneath it, such as tips over search results.
 */
@Composable
fun GroupCard(
    modifier: Modifier = Modifier,
    raised: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        container = if (raised) SurfaceContainer.High else SurfaceContainer.Default,
        shape = KatariTheme.shapes.large,
        shadow = raised,
    ) {
        Column(content = content)
    }
}

/**
 * Draws a [GroupCard]'s fill behind this element over [bounds], for a group whose items are laid out one by one, such
 * as a run of rows in a lazy grid. [bounds] returns null while none of the group is in view.
 */
@Composable
fun Modifier.groupCardBehind(bounds: DrawScope.() -> Rect?): Modifier {
    val fill = KatariTheme.colors.surfaceContainer
    val shape = KatariTheme.shapes.large
    return drawBehind {
        val rect = bounds() ?: return@drawBehind
        if (rect.width <= 0f || rect.height <= 0f) return@drawBehind
        translate(left = rect.left, top = rect.top) {
            drawOutline(outline = shape.createOutline(rect.size, layoutDirection, this), color = fill)
        }
    }
}
