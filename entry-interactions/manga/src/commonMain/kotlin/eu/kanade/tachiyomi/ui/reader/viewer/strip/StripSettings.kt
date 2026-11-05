package eu.kanade.tachiyomi.ui.reader.viewer.strip

import androidx.compose.ui.unit.IntSize
import eu.kanade.tachiyomi.ui.reader.viewer.current
import mihon.entry.interactions.manga.reader.image.layout.PageImageLayout
import mihon.entry.interactions.manga.reader.image.layout.isSpread
import mihon.entry.interactions.manga.reader.settings.MangaReaderSettingsBindings
import mihon.entry.interactions.reader.settings.ChapterTransitionMode
import mihon.entry.interactions.reader.settings.MangaReaderSettings
import tachiyomi.core.common.util.system.SpreadSide

/**
 * The long strip's settings as the opened entry has them now, per-series overrides included. Each read is current, so
 * code that runs on an event follows a change without listening for it; what is drawn reads them in composition.
 */
internal class StripSettings(val bindings: MangaReaderSettingsBindings) {
    val cropBorders: Boolean get() = bindings.cropBordersWebtoon.current
    val sidePaddingPercent: Int get() = bindings.webtoonSidePadding.current
    val tapZones: Int get() = bindings.webtoonNavigationMode.current
    val tapInversion: MangaReaderSettings.TappingInvertMode get() = bindings.webtoonNavigationInverted.current
    val doubleTapZoom: Boolean get() = bindings.webtoonDoubleTapZoom.current
    val zoomOutDisabled: Boolean get() = bindings.webtoonDisableZoomOut.current
    val longTapEnabled: Boolean get() = bindings.readWithLongTap.current
    val pageTransitions: Boolean get() = bindings.pageTransitions.current
    val doubleTapMillis: Int get() = bindings.doubleTapAnimSpeed.current
    val chapterTransitionMode: ChapterTransitionMode get() = bindings.chapterTransition.current

    /** How far a scroll may move the strip at once before it hides the menu, in pixels. */
    val menuHideDistance: Int get() = bindings.readerHideThreshold.current.threshold
}

/**
 * How a page of [imageSize] shows in the long strip: a wide page turned to fit the strip's width, or split with its
 * halves stacked, the half read first on top; otherwise whole.
 */
internal fun stripPageLayout(
    imageSize: IntSize,
    bindings: MangaReaderSettingsBindings,
): PageImageLayout = when {
    !imageSize.isSpread -> PageImageLayout.Whole
    bindings.dualPageRotateToFitWebtoon.current ->
        PageImageLayout.Rotated(clockwise = !bindings.dualPageRotateToFitInvertWebtoon.current)
    bindings.dualPageSplitWebtoon.current -> PageImageLayout.Stacked(
        upper = if (bindings.dualPageInvertWebtoon.current) SpreadSide.LEFT else SpreadSide.RIGHT,
    )
    else -> PageImageLayout.Whole
}
