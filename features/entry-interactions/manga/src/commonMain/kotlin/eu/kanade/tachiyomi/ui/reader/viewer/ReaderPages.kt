package eu.kanade.tachiyomi.ui.reader.viewer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import app.katari.entry.interactions.manga.reader.settings.MangaReaderSettingsBindings
import app.katari.entry.interactions.reader.settings.ReadingMode
import app.katari.presentation.kit.content.PageBackground
import app.katari.presentation.kit.content.PageCanvas
import eu.kanade.tachiyomi.ui.reader.screen.MangaReaderController
import eu.kanade.tachiyomi.ui.reader.viewer.pager.Content
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagedViewer
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerDirection
import eu.kanade.tachiyomi.ui.reader.viewer.strip.Content
import eu.kanade.tachiyomi.ui.reader.viewer.strip.StripViewer

/**
 * The viewer for [mode], a resolved reading mode, showing the pages for [controller]. [openPageInWebView] opens a
 * page's image address where the platform can, or is null where it can't.
 */
internal fun ReadingMode.viewer(
    controller: MangaReaderController,
    openPageInWebView: ((url: String, sourceId: Long?) -> Unit)?,
): ReaderViewer = when (this) {
    ReadingMode.LEFT_TO_RIGHT -> PagedViewer(controller, PagerDirection.LeftToRight, openPageInWebView)
    ReadingMode.RIGHT_TO_LEFT -> PagedViewer(controller, PagerDirection.RightToLeft, openPageInWebView)
    ReadingMode.VERTICAL -> PagedViewer(controller, PagerDirection.Vertical, openPageInWebView)
    ReadingMode.WEBTOON -> StripViewer(controller, continuous = true, openPageInWebView)
    ReadingMode.CONTINUOUS_VERTICAL -> StripViewer(controller, continuous = false, openPageInWebView)
    ReadingMode.DEFAULT -> error("The reading mode must be resolved before a viewer is made for it")
}

/**
 * [viewer]'s pages on the background and in the colours the reader's settings pick. While [tapZonesHeld], something
 * covers the pages and the tap zones wait to show until it is gone.
 */
@Composable
internal fun ReaderPages(
    viewer: ReaderViewer,
    settings: MangaReaderSettingsBindings,
    modifier: Modifier = Modifier,
    tapZonesHeld: Boolean = false,
) {
    val theme by settings.readerTheme.state.collectAsState()
    val grayscale by settings.grayscale.state.collectAsState()
    val inverted by settings.invertedColors.state.collectAsState()
    PageCanvas(
        background = pageBackground(theme.effectiveValue),
        grayscale = grayscale.effectiveValue,
        inverted = inverted.effectiveValue,
        modifier = modifier.fillMaxSize(),
    ) {
        // A viewer replacing one of the same class (left to right after right to left, say) must not inherit its
        // composition: effects keyed on the window, such as the one telling the paged viewer it has two panes,
        // would not run again for it
        key(viewer) {
            when (viewer) {
                is PagedViewer -> viewer.Content(tapZonesHeld = tapZonesHeld)
                is StripViewer -> viewer.Content(tapZonesHeld = tapZonesHeld)
                else -> error("No content for ${viewer::class.simpleName}")
            }
        }
    }
}

/** Whether the reader theme follows each page's own margins, which the viewers then read from the page. */
internal val MangaReaderSettingsBindings.automaticBackground: Boolean
    get() = readerTheme.current == AUTOMATIC_THEME

// As the reader theme setting numbers them
private fun pageBackground(readerTheme: Int): PageBackground = when (readerTheme) {
    0 -> PageBackground.White
    2 -> PageBackground.Gray
    AUTOMATIC_THEME -> PageBackground.Automatic
    else -> PageBackground.Black
}

private const val AUTOMATIC_THEME = 3
