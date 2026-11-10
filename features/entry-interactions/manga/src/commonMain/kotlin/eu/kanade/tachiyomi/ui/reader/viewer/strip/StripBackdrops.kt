package eu.kanade.tachiyomi.ui.reader.viewer.strip

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import app.katari.entry.interactions.manga.reader.image.background.BackdropSpan
import app.katari.entry.interactions.manga.reader.image.background.RowProfile
import app.katari.entry.interactions.manga.reader.image.background.brush
import app.katari.entry.interactions.manga.reader.image.background.rowProfiles
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
 * The space beside the strip's pages when the reader theme is automatic. The [RowProfile]s of each page's pieces are
 * read once, from the page as it shows ([FollowPageBackdrop]) or, for the pages just past either end of the screen,
 * from its image alone ([ReadNeighbourBackdrops]), and kept while the page is in the strip; the rows each run of
 * consecutive read pages covers are worked out over the run as one picture ([stripBackdropRows]), as a long strip is
 * one picture cut into pages, so the top of a page follows the page above it even while that page is off screen; and
 * [paintedBehind] paints what continues each row beside the pages, so the space beside a strip narrower than the
 * window follows the pages as they scroll and zoom. [items] are the strip's slots in order.
 */
internal class StripBackdrops(items: () -> List<ReaderViewerItem>) {
    private val profiles = mutableStateMapOf<ReaderPage, List<PieceProfile>>()
    private val heights = mutableStateMapOf<ReaderPage, Int>()
    private val rows by derivedStateOf { stripBackdropRows(items(), profiles) }

    operator fun get(page: ReaderPage): List<StripBackdropRow> = rows[page].orEmpty()

    /** How many strip pixels tall [page] is laid out while it is in the strip's layout, else 0. */
    fun height(page: ReaderPage): Int = heights[page] ?: 0

    fun has(page: ReaderPage): Boolean = page in profiles

    fun keep(page: ReaderPage, pieces: List<PieceProfile>) {
        if (pieces.isNotEmpty()) profiles[page] = pieces
    }

    /** Forgets the profiles of pages other than [pages], as once their chapter leaves the strip. */
    fun keepOnly(pages: Set<ReaderPage>) {
        profiles.keys.retainAll(pages)
    }

    /** Forgets every profile, as once the settings that open pages change and the pages are read again. */
    fun forget() {
        profiles.clear()
    }

    fun laidOut(page: ReaderPage, height: Int) {
        heights[page] = height
    }

    fun gone(page: ReaderPage, height: Int) {
        if (heights[page] == height) heights.remove(page)
    }
}

/**
 * Keeps the profiles of [page]'s pieces while it shows [shown], read after the page shows so it never waits for them,
 * and tells the strip that [page] is laid out [height] strip pixels tall.
 */
@Composable
internal fun StripViewer.FollowPageBackdrop(page: ReaderPage, shown: PageContent.Shown?, height: Int) {
    val automatic = settings.bindings.automaticBackgroundState()
    LaunchedEffect(shown, automatic) {
        val image = shown?.image as? PageImage.Still ?: return@LaunchedEffect
        if (automatic) backdrops.keep(page, image.pieceProfiles(shown.layout))
    }
    DisposableEffect(page, height) {
        backdrops.laidOut(page, height)
        onDispose { backdrops.gone(page, height) }
    }
}

/**
 * Where each piece of [layout] sits in the page, as fractions of its height, with the piece's profile. None when a
 * piece can't be read, so a page not wholly read ends a run of pages.
 */
internal suspend fun PageImage.Still.pieceProfiles(layout: PageImageLayout): List<PieceProfile> {
    val profiles = rowProfiles(layout)
    val displayedHeight = layout.displayedSize(size).height
    val pieces = layout.pieces(size)
    if (displayedHeight <= 0 || profiles.size != pieces.size || null in profiles) return emptyList()
    return pieces.zip(profiles.filterNotNull()) { piece, profile ->
        val bounds = piece.displayedBounds
        PieceProfile(
            top = bounds.top.toFloat() / displayedHeight,
            bottom = bounds.bottom.toFloat() / displayedHeight,
            profile = profile,
        )
    }
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
        val height = viewer.backdrops.height(page)
        for (row in viewer.backdrops[page]) {
            val rowTop = (slot.offset + row.top * height) * scale
            val rowHeight = (row.bottom - row.top) * height * scale
            paintSide(row.spans, rowTop, rowHeight, left = 0f, right = pagesLeft)
            paintSide(row.spans, rowTop, rowHeight, left = pagesRight, right = size.width)
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
        // A gradient runs from the origin of the area it is drawn in, which is the whole reader, so the span is drawn
        // from its own top with a fade as tall as the span
        val height = (bottom - top).toFloat()
        translate(top = top.toFloat()) {
            drawRect(span.brush(height), Offset(left, 0f), Size(right - left, height))
        }
    }
}
