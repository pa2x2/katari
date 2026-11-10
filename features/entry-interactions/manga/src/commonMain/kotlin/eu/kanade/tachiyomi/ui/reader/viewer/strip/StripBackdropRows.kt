package eu.kanade.tachiyomi.ui.reader.viewer.strip

import app.katari.entry.interactions.manga.reader.image.background.BackdropSpan
import app.katari.entry.interactions.manga.reader.image.background.RowProfile
import app.katari.entry.interactions.manga.reader.image.background.chooseSideBackdrop
import app.katari.entry.interactions.manga.reader.image.background.heightAt
import app.katari.entry.interactions.manga.reader.image.background.joined
import app.katari.entry.interactions.manga.reader.image.background.within
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderViewerItem

/** A piece of a page, from [top] to [bottom] as fractions of the page's height, and its [profile]. */
internal class PieceProfile(val top: Float, val bottom: Float, val profile: RowProfile)

/** A row of a page, from [top] to [bottom] as fractions of the page's height, that [spans] continue at its sides. */
internal class StripBackdropRow(val top: Float, val bottom: Float, val spans: List<BackdropSpan>)

/**
 * The rows of each page in [profiles], worked out over each run of consecutive pages of [items] that all have
 * profiles, read as one picture: a page's artwork and background run on across where the strip was cut into pages, so
 * the space beside the top of a page follows the page above it. A transition or a page not yet read ends a run.
 */
internal fun stripBackdropRows(
    items: List<ReaderViewerItem>,
    profiles: Map<ReaderPage, List<PieceProfile>>,
): Map<ReaderPage, List<StripBackdropRow>> {
    val rows = mutableMapOf<ReaderPage, List<StripBackdropRow>>()
    val run = mutableListOf<Pair<ReaderPage, List<PieceProfile>>>()
    fun endRun() {
        if (run.isNotEmpty()) rows += runRows(run)
        run.clear()
    }
    for (item in items) {
        val page = (item as? ReaderViewerItem.Page)?.page
        val pieces = page?.let(profiles::get)
        if (page == null || pieces.isNullOrEmpty()) endRun() else run += page to pieces
    }
    endRun()
    return rows
}

private fun runRows(run: List<Pair<ReaderPage, List<PieceProfile>>>): Map<ReaderPage, List<StripBackdropRow>> {
    val width = run.maxOf { (_, pieces) -> pieces.maxOf { it.profile.width } }
    val joined = run.flatMap { (_, pieces) -> pieces.map { it.profile } }.joined(width)
    if (joined.height == 0) return emptyMap()
    val spans = chooseSideBackdrop(joined)
    var row = 0
    return run.associate { (page, pieces) ->
        page to pieces.map { piece ->
            val from = row.toFloat() / joined.height
            row += piece.profile.heightAt(width)
            val to = row.toFloat() / joined.height
            StripBackdropRow(piece.top, piece.bottom, spans.mapNotNull { it.within(from, to) })
        }
    }
}
