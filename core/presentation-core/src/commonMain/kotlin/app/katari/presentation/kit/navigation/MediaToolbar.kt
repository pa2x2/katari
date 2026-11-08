package app.katari.presentation.kit.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.Constraints
import app.katari.presentation.kit.theme.KatariTheme

/**
 * A row of tool buttons laid over media, such as a player's controls under its timeline: the leading ones from the
 * start, the [trailing][mediaToolbarSlot] ones against the end. When the row runs out of room, [MediaToolbarButton]s
 * show only their icon, the lowest `labelDropOrder` first, and the trailing buttons are kept whole before the leading
 * ones share what is left, so the actions at the end stay reachable in a narrow window; leading buttons left no room
 * are left out. A slot that [fills leftover room][mediaToolbarSlot], such as the name of the chapter playing, takes
 * only what the buttons leave once each shows its label, and is left out when that is narrower than it can be.
 */
@Composable
fun MediaToolbar(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val gap = KatariTheme.spacing.xs
    Layout(content = content, modifier = modifier.fillMaxWidth()) { measurables, constraints ->
        val gapPx = gap.roundToPx()
        val slots = measurables.map { it.layoutId as? ToolbarSlot ?: ToolbarSlot.Leading }
        // A slot that fills leftover room asks for none of its own, so no label drops to make room for it
        val widths = measurables.mapIndexed { index, measurable ->
            if (slots[index].fillsLeftover) 0 else measurable.maxIntrinsicWidth(constraints.maxHeight)
        }.toMutableList()
        val gaps = gapPx * (measurables.size - 1).coerceAtLeast(0)
        val rowWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else widths.sum() + gaps
        slots.indices
            .filter { slots[it].labelDropOrder != null }
            .sortedBy { slots[it].labelDropOrder }
            .forEach { index ->
                if (widths.sum() + gaps > rowWidth) {
                    widths[index] = measurables[index].minIntrinsicWidth(constraints.maxHeight)
                }
            }

        var remaining = (rowWidth - gaps).coerceAtLeast(0)
        val placeables = arrayOfNulls<Placeable>(measurables.size)
        slots.indices.sortedBy { slots[it].measureTurn }.forEach { index ->
            val width = if (slots[index].fillsLeftover) {
                val measurable = measurables[index]
                measurable.maxIntrinsicWidth(constraints.maxHeight).coerceAtMost(remaining)
                    .takeIf { it >= measurable.minIntrinsicWidth(constraints.maxHeight) }
                    ?: 0
            } else {
                widths[index].coerceIn(0, remaining)
            }
            val placeable = measurables[index].measure(Constraints(maxWidth = width, maxHeight = constraints.maxHeight))
            placeables[index] = placeable
            remaining = (remaining - placeable.width).coerceAtLeast(0)
        }
        val measured = placeables.map { requireNotNull(it) }
        val height = (measured.maxOfOrNull { it.height } ?: 0).coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(rowWidth, height) {
            var start = 0
            var end = rowWidth
            measured.forEachIndexed { index, placeable ->
                if (!slots[index].trailing && placeable.width > 0) {
                    placeable.placeRelative(start, (height - placeable.height) / 2)
                    start += placeable.width + gapPx
                }
            }
            measured.indices.reversed().forEach { index ->
                val placeable = measured[index]
                if (slots[index].trailing) {
                    end -= placeable.width
                    placeable.placeRelative(end, (height - placeable.height) / 2)
                    end -= gapPx
                }
            }
        }
    }
}

/**
 * Where a button of a [MediaToolbar] goes: against the end when [trailing], and, for a [MediaToolbarButton] or a
 * [MediaToolbarPosition], how early it shortens when the row is short of room, lowest first; null keeps the button
 * whole. [fillsLeftover] gives the slot only the room the other buttons leave, as for a chapter name after the time.
 * A kit button that wraps itself takes the slot from a box around it.
 */
fun Modifier.mediaToolbarSlot(
    trailing: Boolean = false,
    labelDropOrder: Int? = null,
    fillsLeftover: Boolean = false,
): Modifier = layoutId(ToolbarSlot(trailing, labelDropOrder, fillsLeftover))

private data class ToolbarSlot(val trailing: Boolean, val labelDropOrder: Int?, val fillsLeftover: Boolean) {
    // The trailing buttons are measured first, then the leading ones, and what fills leftover room last
    val measureTurn: Int
        get() = when {
            trailing -> 0
            fillsLeftover -> 2
            else -> 1
        }

    companion object {
        val Leading = ToolbarSlot(trailing = false, labelDropOrder = null, fillsLeftover = false)
    }
}
