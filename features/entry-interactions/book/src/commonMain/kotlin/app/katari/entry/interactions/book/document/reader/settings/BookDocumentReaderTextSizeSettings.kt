package app.katari.entry.interactions.book.document.reader.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import app.katari.entry.interactions.reader.settings.BookDocumentReaderSettings
import app.katari.entry.viewer.settings.ViewerSettingBinding
import app.katari.presentation.kit.list.StepperRow
import kotlinx.coroutines.launch
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun BookDocumentReaderTextSizeSettings(
    binding: ViewerSettingBinding<Int>,
) {
    val setting by binding.state.collectAsState()
    val scope = rememberCoroutineScope()
    val value = setting.effectiveValue

    fun updateValue(target: Int) {
        scope.launch { binding.setBookTextSize(target) }
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
        setting = binding.definition.profilePreference,
    )
}

/** The text size this book is read at without a size of its own: the profile's, or the reader's default. */
internal val ViewerSettingBinding<Int>.profileTextSize: Int
    get() = state.value.let { it.profileValue ?: it.processorDefault }

/** Gives this book [target] as its text size, or none of its own when that is the profile's. */
internal suspend fun ViewerSettingBinding<Int>.setBookTextSize(target: Int) {
    if (target == profileTextSize) clearEntryOverride() else setEntryOverride(target)
}
