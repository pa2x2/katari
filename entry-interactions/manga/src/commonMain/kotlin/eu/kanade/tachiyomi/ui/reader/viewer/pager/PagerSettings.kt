package eu.kanade.tachiyomi.ui.reader.viewer.pager

import eu.kanade.tachiyomi.ui.reader.viewer.current
import mihon.entry.interactions.manga.reader.settings.MangaReaderSettingsBindings
import mihon.entry.interactions.reader.settings.ChapterTransitionMode
import mihon.entry.interactions.reader.settings.MangaReaderSettings
import mihon.entry.interactions.reader.settings.ReaderPageLayout
import mihon.entry.viewer.settings.ViewerSettingBinding

/**
 * The paged viewer's settings as the opened entry has them now, per-series overrides included. Each read is current,
 * so code that runs on an event, such as a tap, follows a change without listening for it; what is drawn reads them in
 * composition through [ViewerSettingBinding.state].
 */
internal class PagerSettings(private val settings: MangaReaderSettingsBindings, private val direction: PagerDirection) {
    val bindings: MangaReaderSettingsBindings get() = settings

    /** The reader theme that follows each page's own margins. */
    val automaticBackground: Boolean get() = settings.readerTheme.current == AUTOMATIC_THEME
    val scaleType: PageScaleType get() = PageScaleType.fromSetting(settings.imageScaleType.current)
    val cropBorders: Boolean get() = settings.cropBorders.current
    val landscapeZoom: Boolean get() = settings.landscapeZoom.current
    val navigateToPan: Boolean get() = settings.navigateToPan.current
    val pageLayout: ReaderPageLayout get() = settings.pagerPageLayout.current
    val longTapEnabled: Boolean get() = settings.readWithLongTap.current
    val pageTransitions: Boolean get() = settings.pageTransitions.current
    val doubleTapMillis: Int get() = settings.doubleTapAnimSpeed.current
    val chapterTransitionMode: ChapterTransitionMode get() = settings.chapterTransition.current
    val tapZones: Int get() = settings.pagerNavigationMode.current
    val tapInversion: MangaReaderSettings.TappingInvertMode get() = settings.pagerNavigationInverted.current

    /** Automatic starts a page at the side reading starts from, and in the middle of a vertical pager's page. */
    val startPosition: PageStartPosition
        get() = when (settings.zoomStart.current) {
            1 -> when (direction) {
                PagerDirection.LeftToRight -> PageStartPosition.Left
                PagerDirection.RightToLeft -> PageStartPosition.Right
                PagerDirection.Vertical -> PageStartPosition.Center
            }
            2 -> PageStartPosition.Left
            3 -> PageStartPosition.Right
            else -> PageStartPosition.Center
        }

    fun spreadSettings(showsSpreads: Boolean) = PagerSpreadSettings(
        showsSpreads = showsSpreads,
        rotateToFit = settings.dualPageRotateToFit.current,
        rotateToFitInvert = settings.dualPageRotateToFitInvert.current,
        split = settings.dualPageSplitPaged.current,
        splitInvert = settings.dualPageInvertPaged.current,
    )
}

private const val AUTOMATIC_THEME = 3
