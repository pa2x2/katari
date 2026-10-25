package mihon.presentation.kit.list

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.MultiContentMeasurePolicy
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import mihon.presentation.kit.theme.KatariTheme

/**
 * Lays out a row's [leading] content, its [words] with [below] under them, and its [trailing] controls side by side,
 * centred vertically. [leadingGap] follows each leading node and [trailingGap] precedes the controls. A slot that emits
 * nothing is left out, with its gap.
 *
 * When the controls would leave the words narrower than their longest word, so a title would break mid-word or be cut
 * to its first letters, the controls move to a line of their own under the text, still at the row's end. Only narrow
 * screens and large fonts get there; at other widths this is a plain row. [below] doesn't count towards that width: it
 * is arbitrary content, such as a progress bar, that fills whatever width the text gets.
 */
@Composable
internal fun TrailingControlsLayout(
    leading: @Composable () -> Unit,
    words: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
    leadingGap: Dp,
    trailingGap: Dp,
    modifier: Modifier = Modifier,
    below: @Composable () -> Unit = {},
) {
    val stackedGap = KatariTheme.spacing.s
    Layout(
        contents = listOf(leading, words, below, trailing),
        modifier = modifier,
        measurePolicy = MultiContentMeasurePolicy { (leadingSlot, wordsSlot, belowSlot, trailingSlot), constraints ->
            val leadingGapPx = leadingGap.roundToPx()
            val trailingGapPx = trailingGap.roundToPx()
            val loose = constraints.copy(minWidth = 0, minHeight = 0)
            val leadingPlaceables = leadingSlot.map { it.measure(loose) }
            val textStart = leadingPlaceables.sumOf { it.width + leadingGapPx }
            val trailingMeasurable = trailingSlot.firstOrNull()

            if (!constraints.hasBoundedWidth) {
                val trailingPlaceable = trailingMeasurable?.measure(loose)
                val text = measureText(wordsSlot, belowSlot, loose)
                val width = textStart + text.width + (trailingPlaceable?.let { it.width + trailingGapPx } ?: 0)
                return@MultiContentMeasurePolicy placeInline(
                    width,
                    constraints,
                    leadingPlaceables,
                    leadingGapPx,
                    text,
                    trailingPlaceable,
                )
            }

            val width = constraints.maxWidth
            val textRoom = (width - textStart).coerceAtLeast(0)
            val stacked = trailingMeasurable != null &&
                textRoom - trailingGapPx - trailingMeasurable.maxIntrinsicWidth(Constraints.Infinity) <
                (wordsSlot.maxOfOrNull { it.minIntrinsicWidth(Constraints.Infinity) } ?: 0)

            if (stacked) {
                val trailingPlaceable = trailingMeasurable.measure(loose.copy(maxWidth = textRoom))
                val text = measureText(wordsSlot, belowSlot, loose.copy(maxWidth = textRoom))
                val topHeight = maxOf(leadingPlaceables.maxOfOrNull { it.height } ?: 0, text.height)
                val height = (topHeight + stackedGap.roundToPx() + trailingPlaceable.height)
                    .coerceIn(constraints.minHeight, constraints.maxHeight)
                layout(width, height) {
                    placeLeading(leadingPlaceables, leadingGapPx, topHeight)
                    text.place(this, textStart, (topHeight - text.height) / 2)
                    trailingPlaceable.placeRelative(width - trailingPlaceable.width, height - trailingPlaceable.height)
                }
            } else {
                val trailingPlaceable = trailingMeasurable?.measure(
                    loose.copy(maxWidth = (textRoom - trailingGapPx).coerceAtLeast(0)),
                )
                val textWidth = trailingPlaceable?.let { (textRoom - trailingGapPx - it.width).coerceAtLeast(0) }
                    ?: textRoom
                val text = measureText(wordsSlot, belowSlot, loose.copy(maxWidth = textWidth))
                placeInline(width, constraints, leadingPlaceables, leadingGapPx, text, trailingPlaceable)
            }
        },
    )
}

/** The words and what is below them, stacked at the text's start. */
private class TextBlock(private val lines: List<Placeable>) {
    val width = lines.maxOfOrNull { it.width } ?: 0
    val height = lines.sumOf { it.height }

    fun place(scope: Placeable.PlacementScope, x: Int, y: Int) = with(scope) {
        var top = y
        lines.forEach {
            it.placeRelative(x, top)
            top += it.height
        }
    }
}

private fun measureText(words: List<Measurable>, below: List<Measurable>, constraints: Constraints) =
    TextBlock((words + below).map { it.measure(constraints) })

private fun Placeable.PlacementScope.placeLeading(leading: List<Placeable>, gap: Int, height: Int) {
    var x = 0
    leading.forEach {
        it.placeRelative(x, (height - it.height) / 2)
        x += it.width + gap
    }
}

private fun MeasureScope.placeInline(
    width: Int,
    constraints: Constraints,
    leading: List<Placeable>,
    leadingGap: Int,
    text: TextBlock,
    trailing: Placeable?,
): MeasureResult {
    val height = maxOf(leading.maxOfOrNull { it.height } ?: 0, text.height, trailing?.height ?: 0)
        .coerceIn(constraints.minHeight, constraints.maxHeight)
    return layout(width, height) {
        placeLeading(leading, leadingGap, height)
        text.place(this, leading.sumOf { it.width + leadingGap }, (height - text.height) / 2)
        trailing?.placeRelative(width - trailing.width, (height - trailing.height) / 2)
    }
}
