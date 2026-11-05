package mihon.entry.interactions.manga.reader.image.tiles

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.flow.collectLatest
import mihon.entry.interactions.manga.reader.image.layout.PageImageLayout
import mihon.entry.interactions.manga.reader.image.layout.inPiece
import mihon.entry.interactions.manga.reader.image.source.PageImage

/**
 * Draws a still page [image] in [layout], stretched over the composable's size, from tiles decoded at the resolution
 * [view] calls for. The caller sizes the composable to the displayed page's aspect ratio and reports, through [view],
 * which part of it is on screen and how far it is zoomed; a page off screen reports an empty visible area.
 */
@Composable
internal fun TiledPageImage(
    image: PageImage.Still,
    layout: PageImageLayout,
    view: () -> PageImageView,
    modifier: Modifier = Modifier,
) {
    val store = remember(image, layout) { PageTileStore(image, layout) }
    DisposableEffect(store) { onDispose { store.clear() } }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    val currentView by rememberUpdatedState(view)
    LaunchedEffect(store) {
        snapshotFlow { currentView() to boxSize }.collectLatest { (view, size) -> store.load(this, view, size) }
    }
    Canvas(modifier.onSizeChanged { boxSize = it }) {
        if (store.displayedSize.width <= 0) return@Canvas
        val boxScale = size.width / store.displayedSize.width
        scale(boxScale, pivot = Offset.Zero) {
            // Coarse tiles first, so sharper ones cover them where both are decoded
            val tiles = store.decoded.entries.sortedByDescending { it.key.sampleSize }
            store.pieces.forEach { piece ->
                inPiece(piece) {
                    tiles.forEach { (tile, decoded) ->
                        if (piece.source.overlaps(tile.region)) {
                            drawImage(
                                image = decoded.image,
                                dstOffset = (tile.region.topLeft - piece.source.topLeft),
                                dstSize = tile.region.size,
                                filterQuality = FilterQuality.Medium,
                            )
                        }
                    }
                }
            }
        }
    }
}
