package eu.kanade.tachiyomi.ui.reader.viewer.strip

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.IntSize
import app.katari.entry.interactions.manga.reader.image.background.PageBackground
import app.katari.entry.interactions.manga.reader.image.background.brush
import app.katari.entry.interactions.manga.reader.image.layout.PageImageLayout
import app.katari.entry.interactions.manga.reader.image.layout.displayedSize
import app.katari.entry.interactions.manga.reader.image.layout.pieces
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderViewerItem

/**
 * The backgrounds that follow the strip's pages when the reader theme is automatic: each page shown registers the
 * rows its pieces cover, and [paintedBehind] paints each row across the whole screen, so the space beside a strip
 * narrower than the window continues the page beside it as the strip scrolls and zooms.
 */
internal class StripBackdrops {
    private val rows = mutableStateMapOf<ReaderPage, List<StripBackdropRow>>()

    operator fun get(page: ReaderPage): List<StripBackdropRow> = rows[page].orEmpty()

    fun show(page: ReaderPage, pageRows: List<StripBackdropRow>) {
        rows[page] = pageRows
    }

    /** Stops painting [pageRows] for [page], unless the page has registered others since. */
    fun hide(page: ReaderPage, pageRows: List<StripBackdropRow>) {
        if (rows[page] === pageRows) rows.remove(page)
    }
}

/** A row of a page, from [top] to [bottom] in strip pixels below the page's top, that [background] fills. */
internal class StripBackdropRow(val top: Float, val bottom: Float, val background: PageBackground)

/**
 * The rows each piece of a page covers for [backgrounds] in the order of [layout]'s pieces, with the page's image
 * [imageSize] shown [width] strip pixels wide.
 */
internal fun stripBackdropRows(
    layout: PageImageLayout,
    imageSize: IntSize,
    backgrounds: List<PageBackground>,
    width: Int,
): List<StripBackdropRow> {
    val displayedWidth = layout.displayedSize(imageSize).width
    if (displayedWidth <= 0) return emptyList()
    val scale = width.toFloat() / displayedWidth
    return layout.pieces(imageSize).zip(backgrounds) { piece, background ->
        StripBackdropRow(piece.displayedBounds.top * scale, piece.displayedBounds.bottom * scale, background)
    }
}

/**
 * Paints the rows of [viewer]'s pages on screen behind the strip, in screen pixels: a strip pixel is [StripZoom.scale]
 * of them down from the strip's top, and each row spans the whole width whatever the zoom pans or narrows.
 */
internal fun Modifier.paintedBehind(viewer: StripViewer): Modifier = drawBehind {
    val scale = viewer.zoom.scale
    for (slot in viewer.listState.layoutInfo.visibleItemsInfo) {
        val page = (viewer.items.getOrNull(slot.index) as? ReaderViewerItem.Page)?.page ?: continue
        for (row in viewer.backdrops[page]) {
            // A gradient spans the area it is drawn in from the origin, so the row is drawn from its own top
            translate(top = (slot.offset + row.top) * scale) {
                drawRect(row.background.brush(), size = Size(size.width, (row.bottom - row.top) * scale))
            }
        }
    }
}
