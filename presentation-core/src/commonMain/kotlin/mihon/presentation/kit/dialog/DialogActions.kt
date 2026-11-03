package mihon.presentation.kit.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.theme.KatariTheme

/**
 * The buttons closing a dialog or sheet. [confirm] is the one filled button (red when destructive) and [dismiss] is
 * tonal; as a pair they share the width, and a lone button takes all of it. [neutral] is a text button at the start,
 * apart from the others, which then keep their own width at the end. Where the three don't fit on one line, as in a
 * narrow side sheet, [neutral] takes a line of its own above the others, which share theirs again.
 */
@Composable
fun DialogActions(
    confirm: DialogAction?,
    modifier: Modifier = Modifier,
    dismiss: DialogAction? = null,
    neutral: DialogAction? = null,
) {
    if (neutral == null) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            dismiss?.let { ActionButton(it, ButtonStyle.Tonal, Modifier.weight(1f)) }
            confirm?.let { ActionButton(it, ButtonStyle.Filled, Modifier.weight(1f)) }
        }
        return
    }
    val spacing = KatariTheme.spacing.s
    Layout(
        content = {
            ActionButton(neutral, ButtonStyle.Text)
            dismiss?.let { ActionButton(it, ButtonStyle.Tonal) }
            confirm?.let { ActionButton(it, ButtonStyle.Filled) }
        },
        modifier = modifier.fillMaxWidth(),
    ) { measurables, constraints ->
        val gap = spacing.roundToPx()
        val width = constraints.maxWidth
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
            val shared = if (ends.isEmpty()) 0 else (width - gap * (ends.size - 1)) / ends.size
            val endPlaceables = ends.map { it.measure(Constraints.fixedWidth(shared)) }
            val endsHeight = endPlaceables.maxOfOrNull { it.height } ?: 0
            val endsTop = if (endPlaceables.isEmpty()) 0 else neutralPlaceable.height + gap
            layout(width, endsTop + endsHeight) {
                neutralPlaceable.place(0, 0)
                endPlaceables.forEachIndexed { index, placeable ->
                    placeable.place(index * (shared + gap), endsTop + (endsHeight - placeable.height) / 2)
                }
            }
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
    )
}
