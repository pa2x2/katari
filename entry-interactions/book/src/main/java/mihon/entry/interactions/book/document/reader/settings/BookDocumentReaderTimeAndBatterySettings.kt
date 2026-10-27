package mihon.entry.interactions.book.document.reader.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import mihon.entry.viewer.settings.ViewerSettingBinding
import mihon.presentation.kit.list.ToggleRow
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Disabled while the status bar shows, which has the time and battery already. */
@Composable
internal fun BookDocumentReaderTimeAndBatterySettings(
    binding: ViewerSettingBinding<Boolean>,
    statusBarBinding: ViewerSettingBinding<Boolean>,
) {
    val setting by binding.state.collectAsState()
    val statusBar by statusBarBinding.state.collectAsState()
    val scope = rememberCoroutineScope()
    val profileDefault = setting.profileValue ?: setting.processorDefault
    ToggleRow(
        title = stringResource(MR.strings.pref_book_document_reader_show_time_and_battery),
        subtitle = stringResource(MR.strings.pref_book_document_reader_show_time_and_battery_summary),
        checked = setting.effectiveValue,
        enabled = !statusBar.effectiveValue,
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
