package app.katari.presentation.kit.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.katari.presentation.kit.list.rowGroupEdgeInset
import app.katari.presentation.kit.theme.KatariTheme

/**
 * Short tiles of one size in as many columns as give each at least [TileMinWidth], such as the months of a year on
 * Recaps, each a `DashboardTile`. A short last line keeps the columns' width. The grid keeps the side margin of row
 * group cards, and each tile is handed the modifier that sizes it.
 */
@Composable
fun TileGrid(tiles: List<@Composable (Modifier) -> Unit>, modifier: Modifier = Modifier) {
    val gap = KatariTheme.spacing.s
    BoxWithConstraints(modifier.fillMaxWidth().padding(horizontal = rowGroupEdgeInset())) {
        val columns = ((maxWidth + gap) / (TileMinWidth + gap)).toInt().coerceAtLeast(1)
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            tiles.chunked(columns).forEach { line ->
                Row(
                    Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                ) {
                    line.forEach { tile -> tile(Modifier.weight(1f).fillMaxHeight()) }
                    repeat(columns - line.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

// Room for a month's name and its time on one line each
private val TileMinWidth = 104.dp
