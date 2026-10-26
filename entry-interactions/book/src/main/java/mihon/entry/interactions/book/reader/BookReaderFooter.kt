package mihon.entry.interactions.book.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.entry.viewer.chrome.ui.ReaderProgressIndicator
import mihon.presentation.kit.content.ReadingMetrics
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The time and the battery's charge in percent (null until known), shown under the page. */
internal data class BookReaderDeviceStatus(val time: String, val batteryPercent: Int?)

internal fun BookReaderProgress?.showsFooter(deviceStatus: BookReaderDeviceStatus?): Boolean =
    deviceStatus != null || this?.usesFooter == true

/**
 * What sits under the page: the progress when it is shown as text, between the time and the battery when
 * [deviceStatus] is set. Progress drawn along the page edge leaves the footer to the time and battery.
 */
@Composable
internal fun BookReaderFooter(
    progress: BookReaderProgress?,
    deviceStatus: BookReaderDeviceStatus?,
    modifier: Modifier = Modifier,
) {
    val footerProgress = progress?.takeIf { it.usesFooter }
    if (deviceStatus == null) {
        footerProgress?.let { BookReaderFooterProgressIndicator(progress = it, modifier = modifier) }
        return
    }
    Box(modifier = modifier.fillMaxWidth().padding(horizontal = ReadingMetrics.blockInset)) {
        ReaderProgressIndicator(text = deviceStatus.time, modifier = Modifier.align(Alignment.CenterStart))
        footerProgress?.let {
            BookReaderFooterProgressIndicator(progress = it, modifier = Modifier.align(Alignment.Center))
        }
        deviceStatus.batteryPercent?.let { percent ->
            ReaderProgressIndicator(
                text = stringResource(MR.strings.book_reader_footer_battery, percent),
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}
