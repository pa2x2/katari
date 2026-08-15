package mihon.entry.interactions.book.document.reader.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import mihon.entry.interactions.book.R
import mihon.entry.viewer.settings.ViewerSettingBinding
import tachiyomi.presentation.core.components.CheckboxItem

@Composable
internal fun BookDocumentReaderTextSelectionMenuSettings(
    binding: ViewerSettingBinding<Boolean>,
) {
    val setting by binding.state.collectAsState()
    val scope = rememberCoroutineScope()
    val profileDefault = setting.profileValue ?: setting.processorDefault
    CheckboxItem(
        label = stringResource(R.string.book_document_reader_show_text_selection_menu),
        checked = setting.effectiveValue,
        onClick = {
            scope.launch {
                val target = !setting.effectiveValue
                if (target == profileDefault) {
                    binding.clearEntryOverride()
                } else {
                    binding.setEntryOverride(target)
                }
            }
        },
    )
}
