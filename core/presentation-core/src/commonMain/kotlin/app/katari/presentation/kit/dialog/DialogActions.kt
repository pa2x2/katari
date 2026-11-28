package app.katari.presentation.kit.dialog

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.button.shareOrStack
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
            val pair = shareOrStack(measurables, width, gap)
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
            val pair = shareOrStack(ends, width, gap)
            val endsTop = if (ends.isEmpty()) 0 else neutralPlaceable.height + gap
            layout(width, endsTop + pair.height) {
                neutralPlaceable.place(0, 0)
                pair.place(this, endsTop)
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
        loading = action.loading,
    )
}
