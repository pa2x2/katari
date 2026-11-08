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

@Composable
internal fun BookDocumentReaderScreenAliveSettings(
    binding: ViewerSettingBinding<Boolean>,
) {
    val setting by binding.state.collectAsState()
    val scope = rememberCoroutineScope()
    val profileDefault = setting.profileValue ?: setting.processorDefault
    ToggleRow(
        title = stringResource(MR.strings.book_document_reader_keep_screen_alive),
        checked = setting.effectiveValue,
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
