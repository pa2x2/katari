package mihon.presentation.kit.layout

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mihon.presentation.kit.content.entryCoverScale
import tachiyomi.domain.library.model.CoverSize
import kotlin.math.ceil
import kotlin.math.min

/**
 * Columns for a grid of entry covers: as many as fit the width at [size], with covers scaled up in the wide window
 * classes so a desktop window fills with large covers rather than many small ones. Pass [wideCovers] when every cover
 * is landscape, such as video thumbnails.
 */
@Composable
@ReadOnlyComposable
fun coverGridCells(size: CoverSize, wideCovers: Boolean = false): GridCells {
    val minCellWidth = size.minCellWidth() * entryCoverScale()
    return GridCells.Adaptive(if (wideCovers) minCellWidth * WIDE_COVER_SCALE else minCellWidth)
}

/**
 * Columns for covers that a phone shows one to a line, such as the comfortable list display mode. Each cover stays
 * between [CoverListMinWidth] and [CoverListMaxWidth] wide, about a phone's width, so a wider window lays covers side
 * by side rather than stretching one until it is taller than the window.
 */
fun coverListCells(): GridCells = CoverListCells

private object CoverListCells : GridCells {
    override fun Density.calculateCrossAxisCellSizes(availableSize: Int, spacing: Int): List<Int> {
        val width = availableSize.toDp()
        val columns = min((width / CoverListMinWidth).toInt(), ceil(width / CoverListMaxWidth).toInt())
        return with(GridCells.Fixed(columns.coerceAtLeast(1))) { calculateCrossAxisCellSizes(availableSize, spacing) }
    }
}

private val CoverListMinWidth = 320.dp
private val CoverListMaxWidth = 480.dp

/** How many columns [this] lays out across [availableWidth], for content that has to know before the grid does. */
@Composable
@ReadOnlyComposable
fun GridCells.columnCount(availableWidth: Dp, spacing: Dp): Int {
    val density = LocalDensity.current
    return with(density) {
        calculateCrossAxisCellSizes(availableWidth.roundToPx().coerceAtLeast(0), spacing.roundToPx()).size
    }
}

// Narrowest a cell gets. The sizes keep the column count people had chosen before there were sizes, 4 to 1 per row,
// on a typical phone about 411 dp wide.
private fun CoverSize.minCellWidth(): Dp = when (this) {
    CoverSize.Auto -> 128.dp
    CoverSize.Small -> 88.dp
    CoverSize.Medium -> 120.dp
    CoverSize.Large -> 150.dp
    CoverSize.ExtraLarge -> 200.dp
}

// Landscape covers are shorter for their width, so their cells are wider by the ratio the automatic size always used,
// 180 dp against 128 dp.
private const val WIDE_COVER_SCALE = 180f / 128f
