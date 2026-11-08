package app.katari.presentation.kit.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.katari.presentation.kit.theme.KatariTheme

/**
 * A dashboard's headline tiles, such as `DashboardTile`s with the time spent and the streak: all in one line where each
 * gets at least [TileMinWidth], two to a line otherwise. The tiles of a line share its width and height, so a short
 * last line's tile takes the whole line. Each tile is handed the modifier that sizes it.
 */
@Composable
fun DashboardTiles(tiles: List<@Composable (Modifier) -> Unit>, modifier: Modifier = Modifier) {
    val gap = KatariTheme.spacing.s
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val oneLine = TileMinWidth * tiles.size + gap * (tiles.size - 1) <= maxWidth
        val columns = if (oneLine) tiles.size else 2
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            tiles.chunked(columns.coerceAtLeast(1)).forEach { line ->
                Row(
                    Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                ) {
                    line.forEach { tile -> tile(Modifier.weight(1f).fillMaxHeight()) }
                }
            }
        }
    }
}

// Room for a figure and a two-line note under it
private val TileMinWidth = 200.dp
