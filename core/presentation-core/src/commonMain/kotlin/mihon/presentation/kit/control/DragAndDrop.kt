package mihon.presentation.kit.control

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariColors
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.LocalKatariColors

/** The edge of an item a dragged item would be inserted at. */
enum class DropEdge { Start, End, Top, Bottom }

/**
 * Draws a line on [edge] of an item to show where a dragged item would be inserted, in the error colour when the drop
 * would be rejected. Usable outside composition, such as in a per-item modifier lambda.
 */
fun Modifier.dropInsertionIndicator(visible: Boolean, edge: DropEdge, valid: Boolean): Modifier =
    if (visible) this then DropInsertionIndicatorElement(edge, valid) else this

private data class DropInsertionIndicatorElement(
    val edge: DropEdge,
    val valid: Boolean,
) : ModifierNodeElement<DropInsertionIndicatorNode>() {
    override fun create() = DropInsertionIndicatorNode(edge, valid)

    override fun update(node: DropInsertionIndicatorNode) {
        node.edge = edge
        node.valid = valid
        node.invalidateDraw()
    }
}

private class DropInsertionIndicatorNode(
    var edge: DropEdge,
    var valid: Boolean,
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {
    override fun ContentDrawScope.draw() {
        drawContent()
        val color = currentValueOf(LocalKatariColors).dropColor(valid)
        val strokeWidth = KatariTheme.sizes.selectedBorder.toPx()
        val inset = KatariTheme.spacing.s.toPx()
        val (start, end) = when (edge) {
            DropEdge.Start, DropEdge.End -> {
                val x = if (edge == DropEdge.Start) strokeWidth / 2 else size.width - strokeWidth / 2
                Offset(x, inset) to Offset(x, size.height - inset)
            }
            DropEdge.Top, DropEdge.Bottom -> {
                val y = if (edge == DropEdge.Top) strokeWidth / 2 else size.height - strokeWidth / 2
                Offset(inset, y) to Offset(size.width - inset, y)
            }
        }
        drawLine(color = color, start = start, end = end, strokeWidth = strokeWidth, cap = StrokeCap.Round)
    }
}

/**
 * A floating panel that dragged items can be dropped into. While a drag is over it ([active]), its outline turns into
 * the drop colour, the error colour when the drop would be rejected.
 */
@Composable
fun DropTargetPanel(
    active: Boolean,
    valid: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = KatariTheme.shapes.large
    Surface(
        modifier = if (active) {
            modifier.border(KatariTheme.sizes.selectedBorder, KatariTheme.colors.dropColor(valid), shape)
        } else {
            modifier
        },
        container = SurfaceContainer.High,
        shape = shape,
        outlined = !active,
        shadow = true,
    ) {
        Column(modifier = Modifier.padding(KatariTheme.spacing.s), content = content)
    }
}

/**
 * The item being dragged, centred under the finger at [pointerPosition] in its parent's coordinates. Tinted as an
 * error while the drop would be rejected.
 */
@Composable
fun DragPreview(
    pointerPosition: Offset,
    valid: Boolean,
    content: @Composable RowScope.() -> Unit,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    Surface(
        modifier = Modifier
            .onSizeChanged { size = it }
            .offset {
                IntOffset(
                    x = (pointerPosition.x - size.width / 2f).toInt(),
                    y = (pointerPosition.y - size.height / 2f).toInt(),
                )
            },
        container = if (valid) SurfaceContainer.PrimaryContainer else SurfaceContainer.ErrorContainer,
        shape = KatariTheme.shapes.large,
        shadow = true,
    ) {
        Row(
            modifier = Modifier.padding(KatariTheme.spacing.m),
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

private fun KatariColors.dropColor(valid: Boolean) = if (valid) primary else error
