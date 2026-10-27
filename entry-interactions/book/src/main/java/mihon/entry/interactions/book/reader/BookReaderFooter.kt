package mihon.entry.interactions.book.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import mihon.entry.viewer.chrome.ui.ReaderPageIndicator
import mihon.entry.viewer.chrome.ui.ReaderProgressIndicator
import mihon.presentation.kit.content.ReadingMetrics
import mihon.presentation.kit.layout.PageSpread
import mihon.presentation.kit.window.currentWindowLayout
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The time and the battery's charge in percent (null until known), shown under the page. */
internal data class BookReaderDeviceStatus(val time: String, val batteryPercent: Int?)

internal fun BookReaderProgress?.showsFooter(deviceStatus: BookReaderDeviceStatus?): Boolean =
    deviceStatus != null || this?.usesFooter == true

/**
 * What sits under the page: the progress when it is shown as text, between the time and the battery when
 * [deviceStatus] is set. Progress drawn along the page edge leaves the footer to the time and battery. Under a two-page
 * spread each page gets its own number, centred under it. Half-open like a book, a single number or percentage sits
 * under the end half rather than on the crease. The footer keeps clear of a side cutout like the pages do, so its
 * middle lines up with theirs.
 */
@Composable
internal fun BookReaderFooter(
    progress: BookReaderProgress?,
    deviceStatus: BookReaderDeviceStatus?,
    modifier: Modifier = Modifier,
) {
    val footerProgress = progress?.takeIf { it.usesFooter }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        contentAlignment = Alignment.Center,
    ) {
        if (deviceStatus != null) {
            ReaderProgressIndicator(
                text = deviceStatus.time,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = ReadingMetrics.blockInset),
            )
            deviceStatus.batteryPercent?.let { percent ->
                ReaderProgressIndicator(
                    text = stringResource(MR.strings.book_reader_footer_battery, percent),
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = ReadingMetrics.blockInset),
                )
            }
        }
        val facingPage = (footerProgress as? BookReaderProgress.Page)?.facingPage
        when {
            footerProgress == null -> Unit
            footerProgress is BookReaderProgress.Page && facingPage != null -> PageSpread(Modifier.fillMaxWidth()) {
                val first: @Composable BoxScope.() -> Unit = {
                    ReaderPageIndicator(
                        footerProgress.currentPage,
                        footerProgress.totalPages,
                        Modifier.align(Alignment.Center),
                    )
                }
                val second: @Composable BoxScope.() -> Unit = {
                    ReaderPageIndicator(facingPage, footerProgress.totalPages, Modifier.align(Alignment.Center))
                }
                Pages(
                    left = if (footerProgress.rightToLeft) second else first,
                    right = if (footerProgress.rightToLeft) first else second,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            currentWindowLayout().fold?.let { it.isVertical && it.isHalfOpen } == true -> PageSpread(
                Modifier.fillMaxWidth(),
            ) {
                val indicator: @Composable BoxScope.() -> Unit = {
                    BookReaderFooterProgressIndicator(
                        progress = footerProgress,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                val rtlLayout = LocalLayoutDirection.current == LayoutDirection.Rtl
                Pages(
                    left = if (rtlLayout) indicator else ({}),
                    right = if (rtlLayout) ({}) else indicator,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            else -> BookReaderFooterProgressIndicator(progress = footerProgress)
        }
    }
}
