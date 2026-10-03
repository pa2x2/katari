package eu.kanade.presentation.library.update

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** "Oct 10, 7:30 PM": when a source's pause ends, with the year only when it isn't this year's. */
@Composable
@ReadOnlyComposable
fun sourcePauseEndText(until: Long): String {
    return DateUtils.formatDateTime(
        LocalContext.current,
        until,
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH,
    )
}

/** "Paused", or "Paused until Oct 10, 7:30 PM" for a pause that ends on its own. */
@Composable
@ReadOnlyComposable
fun sourcePauseStateText(until: Long?): String {
    return if (until == null) {
        stringResource(MR.strings.library_update_source_paused)
    } else {
        stringResource(MR.strings.library_update_source_paused_until, sourcePauseEndText(until))
    }
}
