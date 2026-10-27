package mihon.entry.interactions.book.reader

import androidx.compose.foundation.layout.Box
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
import mihon.entry.viewer.chrome.ui.ReaderPageIndicator
import mihon.entry.viewer.chrome.ui.ReaderProgressIndicator
import mihon.entry.viewer.chrome.ui.ReaderUnderPages
import mihon.presentation.kit.content.ReadingMetrics
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
        if (footerProgress != null) {
            val page = footerProgress as? BookReaderProgress.Page
            val facingPage = page?.facingPage
            ReaderUnderPages(
                modifier = Modifier.fillMaxWidth(),
                facing = if (page != null && facingPage != null) {
                    { ReaderPageIndicator(facingPage, page.totalPages, Modifier.align(Alignment.Center)) }
                } else {
                    null
                },
                rightToLeft = page?.rightToLeft == true,
            ) {
                BookReaderFooterProgressIndicator(
                    progress = footerProgress,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}
