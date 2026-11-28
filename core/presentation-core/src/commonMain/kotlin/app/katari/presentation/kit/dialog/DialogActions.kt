package app.katari.presentation.kit.dialog

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.theme.KatariTheme

/**
 * The buttons closing a dialog or sheet. [confirm] is the one filled button (red when destructive) and [dismiss] is
 * tonal; as a pair they share the width, and a lone button takes all of it. Where a label doesn't fit on one line in
 * its half, as a long or translated one on a phone, the pair stacks instead, each taking the whole width and [confirm]
 * on top. [neutral] is a text button at the start, apart from the others, which then keep their own width at the end.
 * Where the three don't fit on one line, as in a narrow side sheet, [neutral] takes a line of its own above the others,
 * which share theirs again.
 */
@Composable
fun DialogActions(
    confirm: DialogAction?,
    modifier: Modifier = Modifier,
    dismiss: DialogAction? = null,
    neutral: DialogAction? = null,
) {
    val spacing = KatariTheme.spacing.s
    Layout(
        content = {
            neutral?.let { ActionButton(it, ButtonStyle.Text) }
            dismiss?.let { ActionButton(it, ButtonStyle.Tonal) }
            confirm?.let { ActionButton(it, ButtonStyle.Filled) }
        },
        modifier = modifier.fillMaxWidth(),
    ) { measurables, constraints ->
        val gap = spacing.roundToPx()
        val width = constraints.maxWidth
        if (neutral == null) {
            val pair = pair(measurables, width, gap)
            return@Layout layout(width, pair.height) { pair.place(this, 0) }
        }
        val neutralButton = measurables.first()
        val ends = measurables.drop(1)
        val wanted = measurables.sumOf { it.maxIntrinsicWidth(constraints.maxHeight) } + gap * (measurables.size - 1)
        if (wanted <= width) {
            val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
            val height = placeables.maxOf { it.height }
            layout(width, height) {
                placeables.first().place(0, (height - placeables.first().height) / 2)
                var x = width
                placeables.drop(1).asReversed().forEach { placeable ->
                    x -= placeable.width
                    placeable.place(x, (height - placeable.height) / 2)
                    x -= gap
                }
            }
        } else {
            val neutralPlaceable = neutralButton.measure(constraints.copy(minWidth = 0, minHeight = 0))
            val pair = pair(ends, width, gap)
            val endsTop = if (ends.isEmpty()) 0 else neutralPlaceable.height + gap
            layout(width, endsTop + pair.height) {
                neutralPlaceable.place(0, 0)
                pair.place(this, endsTop)
            }
        }
    }
}

private class ActionPair(val height: Int, val place: Placeable.PlacementScope.(top: Int) -> Unit)

// The dismiss and confirm buttons, in that order, across the whole width
private fun pair(buttons: List<Measurable>, width: Int, gap: Int): ActionPair {
    if (buttons.isEmpty()) return ActionPair(0) {}
    val shared = (width - gap * (buttons.size - 1)) / buttons.size
    if (buttons.all { it.maxIntrinsicWidth(Constraints.Infinity) <= shared }) {
        val placeables = buttons.map { it.measure(Constraints.fixedWidth(shared)) }
        val height = placeables.maxOf { it.height }
        return ActionPair(height) { top ->
            placeables.forEachIndexed { index, placeable ->
                placeable.place(index * (shared + gap), top + (height - placeable.height) / 2)
            }
        }
    }
    val stacked = buttons.asReversed().map { it.measure(Constraints.fixedWidth(width)) }
    return ActionPair(stacked.sumOf { it.height } + gap * (stacked.size - 1)) { top ->
        var y = top
        stacked.forEach { placeable ->
            placeable.place(0, y)
            y += placeable.height + gap
        }
    }
}

@Composable
private fun ActionButton(action: DialogAction, style: ButtonStyle, modifier: Modifier = Modifier) {
    Button(
        label = action.label,
        onClick = action.onClick,
        modifier = modifier,
        style = style,
        enabled = action.enabled,
        destructive = action.destructive,
        loading = action.loading,
    )
}
