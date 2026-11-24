package app.katari.presentation.kit.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.katari.presentation.kit.theme.KatariTheme

/**
 * The choices of one setting shown as pictures of them ([tile], a `ChoiceTile` each), such as the display modes of a
 * screen of covers. As many share a line as fit while each stays wide enough for a label of two short words, and the
 * lines are balanced, so five choices sit on one line in a wide sheet and three over two in a popover or phone sheet.
 * Every tile is the same width.
 */
@Composable
fun <T> ChoiceTileGrid(
    options: List<T>,
    modifier: Modifier = Modifier,
    tile: @Composable (T) -> Unit,
) {
    if (options.isEmpty()) return
    val gap = KatariTheme.spacing.s
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup()
            .padding(horizontal = LocalRowInset.current),
    ) {
        val fit = ((maxWidth + gap) / (ChoiceTileMinWidth + gap)).toInt().coerceIn(1, options.size)
        val lineCount = (options.size + fit - 1) / fit
        val perLine = (options.size + lineCount - 1) / lineCount
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            options.chunked(perLine).forEach { line ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    line.forEach { option ->
                        Box(modifier = Modifier.weight(1f), propagateMinConstraints = true) { tile(option) }
                    }
                    repeat(perLine - line.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

private val ChoiceTileMinWidth = 96.dp
