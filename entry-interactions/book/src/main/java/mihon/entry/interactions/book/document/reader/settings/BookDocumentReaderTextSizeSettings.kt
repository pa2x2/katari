package mihon.entry.interactions.book.document.reader.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import mihon.entry.interactions.book.R
import mihon.entry.interactions.reader.settings.BookDocumentReaderSettings
import mihon.entry.viewer.settings.ViewerSettingBinding
import tachiyomi.presentation.core.components.SettingsStepperItem

@Composable
internal fun BookDocumentReaderTextSizeSettings(
    binding: ViewerSettingBinding<Int>,
) {
    val setting by binding.state.collectAsState()
    val scope = rememberCoroutineScope()
    val value = setting.effectiveValue
    val profileDefault = setting.profileValue ?: setting.processorDefault

    fun updateValue(target: Int) {
        scope.launch {
            if (target == profileDefault) {
                binding.clearEntryOverride()
            } else {
                binding.setEntryOverride(target)
            }
        }
    }

    SettingsStepperItem(
        label = stringResource(R.string.book_document_reader_text_size),
        value = value,
        valueRange = BookDocumentReaderSettings.TEXT_SIZE_RANGE,
        step = BookDocumentReaderSettings.TEXT_SIZE_STEP_PERCENT,
        valueFormatter = { "$it%" },
        inputSuffix = "%",
        decreaseContentDescription = stringResource(R.string.book_document_reader_decrease_text_size),
        increaseContentDescription = stringResource(R.string.book_document_reader_increase_text_size),
        editContentDescription = stringResource(R.string.book_document_reader_edit_text_size),
        onValueChange = ::updateValue,
    )
}
