package app.katari.entry.interactions.book.document.reader.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import app.katari.entry.viewer.settings.ViewerSettingBinding
import app.katari.presentation.kit.list.ToggleRow
import kotlinx.coroutines.launch
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Disabled while the status bar shows, which has the time and battery already. */
@Composable
internal fun BookDocumentReaderTimeAndBatterySettings(
    binding: ViewerSettingBinding<Boolean>,
    /** Null where the window has no status bar, which would show the time and battery itself. */
    statusBarBinding: ViewerSettingBinding<Boolean>?,
) {
    val setting by binding.state.collectAsState()
    val statusBarShown = statusBarBinding?.state?.collectAsState()?.value?.effectiveValue == true
    val scope = rememberCoroutineScope()
    val profileDefault = setting.profileValue ?: setting.processorDefault
    ToggleRow(
        title = stringResource(MR.strings.pref_book_document_reader_show_time_and_battery),
        subtitle = stringResource(
            if (statusBarBinding != null) {
                MR.strings.pref_book_document_reader_show_time_and_battery_summary
            } else {
                MR.strings.pref_book_document_reader_show_time_and_battery_summary_without_status_bar
            },
        ),
        checked = setting.effectiveValue,
        enabled = !statusBarShown,
        onCheckedChange = { target ->
            scope.launch {
                if (target == profileDefault) {
                    binding.clearEntryOverride()
                } else {
                    binding.setEntryOverride(target)
                }
            }
        },
    )
}
