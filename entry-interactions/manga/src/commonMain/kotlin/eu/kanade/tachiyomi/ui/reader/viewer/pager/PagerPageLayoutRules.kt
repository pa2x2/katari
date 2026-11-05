package eu.kanade.tachiyomi.ui.reader.viewer.pager

import androidx.compose.ui.unit.IntSize
import eu.kanade.tachiyomi.ui.reader.model.InsertPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import mihon.entry.interactions.manga.reader.image.layout.PageImageLayout
import mihon.entry.interactions.manga.reader.image.layout.isSpread
import tachiyomi.core.common.util.system.SpreadSide

/** The paged viewer's settings for wide pages, read once per page. */
internal data class PagerSpreadSettings(
    val showsSpreads: Boolean,
    val rotateToFit: Boolean,
    val rotateToFitInvert: Boolean,
    val split: Boolean,
    val splitInvert: Boolean,
)

/** How the paged viewer shows a page, and what it found out about it for the pages around it. */
internal data class PagerPageLayout(
    val layout: PageImageLayout,
    /** The page is a wide image beside another while two pages show; it must stand alone. */
    val isWideBesideOther: Boolean = false,
    /** The page is the first half of a wide image split in two; its second half needs a slot. */
    val needsSecondHalf: Boolean = false,
)

/**
 * How [page], an image of [imageSize], shows in a pager turning in [direction]. While two pages show, a wide image is
 * whole and stands alone. Otherwise a wide image is turned to fit, or split into halves shown one after the other: the
 * page shows the half read first and an inserted page the other.
 */
internal fun pagerPageLayout(
    page: ReaderPage,
    imageSize: IntSize,
    direction: PagerDirection,
    settings: PagerSpreadSettings,
): PagerPageLayout {
    val isWide = imageSize.isSpread
    return when {
        settings.showsSpreads -> PagerPageLayout(PageImageLayout.Whole, isWideBesideOther = isWide)
        settings.rotateToFit && isWide -> PagerPageLayout(
            PageImageLayout.Rotated(clockwise = !settings.rotateToFitInvert),
        )
        settings.rotateToFit || !settings.split -> PagerPageLayout(PageImageLayout.Whole)
        page is InsertPage -> PagerPageLayout(PageImageLayout.Half(splitSide(page, direction, settings.splitInvert)))
        isWide -> PagerPageLayout(
            PageImageLayout.Half(splitSide(page, direction, settings.splitInvert)),
            needsSecondHalf = true,
        )
        else -> PagerPageLayout(PageImageLayout.Whole)
    }
}

// Left to right reads the left half first; the other directions read the right half first
private fun splitSide(page: ReaderPage, direction: PagerDirection, invert: Boolean): SpreadSide {
    val first = if (direction == PagerDirection.LeftToRight) SpreadSide.LEFT else SpreadSide.RIGHT
    val side = if (page is InsertPage) first.opposite else first
    return if (invert) side.opposite else side
}
