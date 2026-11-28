package app.katari.presentation.kit.button

import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints

/** Buttons measured by [shareOrStack], placed from a given top. */
internal class SharedButtons(val height: Int, val place: Placeable.PlacementScope.(top: Int) -> Unit)

/**
 * [buttons] across [width]: side by side in equal shares where every label fits its share on one line, otherwise
 * stacked, each as wide as [width] and the last, the main action, on top. A dialog's pair and a screen's main actions
 * are laid out this way, so a long or translated label, or a large font, never wraps inside a button.
 */
internal fun shareOrStack(buttons: List<Measurable>, width: Int, gap: Int): SharedButtons {
    if (buttons.isEmpty()) return SharedButtons(0) {}
    val share = (width - gap * (buttons.size - 1)) / buttons.size
    if (buttons.all { it.maxIntrinsicWidth(Constraints.Infinity) <= share }) {
        val placeables = buttons.map { it.measure(Constraints.fixedWidth(share)) }
        val height = placeables.maxOf { it.height }
        return SharedButtons(height) { top ->
            placeables.forEachIndexed { index, placeable ->
                placeable.place(index * (share + gap), top + (height - placeable.height) / 2)
            }
        }
    }
    val stacked = buttons.asReversed().map { it.measure(Constraints.fixedWidth(width)) }
    return SharedButtons(stacked.sumOf { it.height } + gap * (stacked.size - 1)) { top ->
        var y = top
        stacked.forEach { placeable ->
            placeable.place(0, y)
            y += placeable.height + gap
        }
    }
}
