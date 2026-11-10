package eu.kanade.tachiyomi.ui.reader.viewer.strip

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.IntSize
import app.katari.entry.interactions.manga.reader.image.background.BackdropSpan
import app.katari.entry.interactions.manga.reader.image.background.SideBackdrop
import app.katari.entry.interactions.manga.reader.image.background.brush
import app.katari.entry.interactions.manga.reader.image.background.chooseSideBackdrops
import app.katari.entry.interactions.manga.reader.image.layout.PageImageLayout
import app.katari.entry.interactions.manga.reader.image.layout.displayedSize
import app.katari.entry.interactions.manga.reader.image.layout.pieces
import app.katari.entry.interactions.manga.reader.image.source.PageImage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderViewerItem
import eu.kanade.tachiyomi.ui.reader.viewer.automaticBackgroundState
import eu.kanade.tachiyomi.ui.reader.viewer.page.PageContent
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * The space beside the strip's pages when the reader theme is automatic: each page shown registers the rows its pieces
 * cover, and [paintedBehind] paints what continues each row beside the pages, so the space beside a strip narrower
 * than the window follows the page next to it down its height as the strip scrolls and zooms.
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

/** A row of a page, from [top] to [bottom] in strip pixels below the page's top, that [backdrop] continues. */
internal class StripBackdropRow(val top: Float, val bottom: Float, val backdrop: SideBackdrop)

/**
 * Registers the rows of [page] while it shows [shown], laid out [height] strip pixels tall, once what continues its
 * pieces is read, which happens after the page shows so it never waits for it.
 */
@Composable
internal fun StripViewer.FollowPageBackdrop(page: ReaderPage, shown: PageContent.Shown?, height: Int) {
    val automatic = settings.bindings.automaticBackgroundState()
    var sideBackdrops by remember(shown, automatic) { mutableStateOf(emptyList<SideBackdrop?>()) }
    LaunchedEffect(shown, automatic) {
        val image = shown?.image as? PageImage.Still ?: return@LaunchedEffect
        if (automatic) sideBackdrops = image.chooseSideBackdrops(shown.layout)
    }
    val rows = remember(shown, sideBackdrops, height) {
        shown?.let { stripBackdropRows(it.layout, it.image.size, sideBackdrops, height) }.orEmpty()
    }
    DisposableEffect(page, rows) {
        backdrops.show(page, rows)
        onDispose { backdrops.hide(page, rows) }
    }
}

/**
 * The rows each piece of a page covers for [backdrops] in the order of [layout]'s pieces, with the page's image
 * [imageSize] stretched over [height] strip pixels, the height the page is laid out at, so its last row ends where the
 * next page begins.
 */
internal fun stripBackdropRows(
    layout: PageImageLayout,
    imageSize: IntSize,
    backdrops: List<SideBackdrop?>,
    height: Int,
): List<StripBackdropRow> {
    val displayedHeight = layout.displayedSize(imageSize).height
    if (displayedHeight <= 0 || height <= 0) return emptyList()
    return layout.pieces(imageSize).zip(backdrops) { piece, backdrop ->
        if (backdrop == null) return@zip null
        val bounds = piece.displayedBounds
        StripBackdropRow(
            top = bounds.top.toFloat() * height / displayedHeight,
            bottom = bounds.bottom.toFloat() * height / displayedHeight,
            backdrop = backdrop,
        )
    }.filterNotNull()
}

/**
 * Paints the rows of [viewer]'s pages on screen behind the strip, in screen pixels: a strip pixel is [StripZoom.scale]
 * of them down from the strip's top, and each side fills the screen from its edge to the pages', wherever the zoom
 * pans or narrows them.
 */
internal fun Modifier.paintedBehind(viewer: StripViewer): Modifier = drawBehind {
    val scale = viewer.zoom.scale
    // Each side reaches under the pages' edge to a whole pixel, so no line of the reader's background shows between
    val pagesLeft = ceil(viewer.zoom.pagesLeft)
    val pagesRight = floor(viewer.zoom.pagesRight)
    for (slot in viewer.listState.layoutInfo.visibleItemsInfo) {
        val page = (viewer.items.getOrNull(slot.index) as? ReaderViewerItem.Page)?.page ?: continue
        for (row in viewer.backdrops[page]) {
            val rowTop = (slot.offset + row.top) * scale
            val rowHeight = (row.bottom - row.top) * scale
            paintSide(row.backdrop.left, rowTop, rowHeight, left = 0f, right = pagesLeft)
            paintSide(row.backdrop.right, rowTop, rowHeight, left = pagesRight, right = size.width)
        }
    }
}

/** Paints [spans] of a row [rowHeight] screen pixels tall at [rowTop], between [left] and [right]. */
private fun DrawScope.paintSide(spans: List<BackdropSpan>, rowTop: Float, rowHeight: Float, left: Float, right: Float) {
    if (right <= left) return
    for (span in spans) {
        // Spans end on whole pixels: spans meeting at a fractional one would each cover it only partly, antialiased,
        // and leave a line of the reader's background between them
        val top = (rowTop + span.top * rowHeight).roundToInt()
        val bottom = (rowTop + span.bottom * rowHeight).roundToInt()
        if (bottom <= top) continue
        // A gradient spans the area it is drawn in from the origin, so the span is drawn from its own top
        translate(top = top.toFloat()) {
            drawRect(span.brush(), Offset(left, 0f), Size(right - left, (bottom - top).toFloat()))
        }
    }
}
