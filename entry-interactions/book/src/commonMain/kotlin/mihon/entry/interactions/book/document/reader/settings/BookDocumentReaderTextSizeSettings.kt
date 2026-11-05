package mihon.entry.interactions.book.document.reader.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import mihon.entry.interactions.reader.settings.BookDocumentReaderSettings
import mihon.entry.viewer.settings.ViewerSettingBinding
import mihon.presentation.kit.list.StepperRow
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

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

    StepperRow(
        title = stringResource(MR.strings.book_document_reader_text_size),
        value = value,
        valueRange = BookDocumentReaderSettings.TEXT_SIZE_RANGE,
        step = BookDocumentReaderSettings.TEXT_SIZE_STEP_PERCENT,
        valueFormatter = { "$it%" },
        inputSuffix = "%",
        decreaseContentDescription = stringResource(MR.strings.book_document_reader_decrease_text_size),
        increaseContentDescription = stringResource(MR.strings.book_document_reader_increase_text_size),
        editContentDescription = stringResource(MR.strings.book_document_reader_edit_text_size),
        onValueChange = ::updateValue,
    )
}
