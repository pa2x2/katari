package mihon.entry.interactions.book.document.reader.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import mihon.entry.interactions.book.R
import mihon.entry.viewer.settings.ViewerSettingBinding
import mihon.presentation.kit.list.ChoiceRow
import mihon.presentation.kit.list.SectionHeader

@Composable
internal fun BookDocumentReaderThemeSettings(
    binding: ViewerSettingBinding<BookDocumentReaderThemeMode>,
) {
    val setting by binding.state.collectAsState()
    val scope = rememberCoroutineScope()
    val profileDefault = setting.profileValue ?: setting.processorDefault
    Column {
        SectionHeader(text = stringResource(R.string.book_document_reader_theme))
        BookDocumentReaderThemeMode.entries.forEach { mode ->
            ChoiceRow(
                title = mode.label(),
                selected = setting.effectiveValue == mode,
                onClick = {
                    scope.launch {
                        if (mode == profileDefault) {
                            binding.clearEntryOverride()
                        } else {
                            binding.setEntryOverride(mode)
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun BookDocumentReaderThemeMode.label(): String = stringResource(
    when (this) {
        BookDocumentReaderThemeMode.APP -> R.string.book_document_reader_theme_app
        BookDocumentReaderThemeMode.PAPER -> R.string.book_document_reader_theme_paper
        BookDocumentReaderThemeMode.DUSK -> R.string.book_document_reader_theme_dusk
        BookDocumentReaderThemeMode.BLACK -> R.string.book_document_reader_theme_black
    },
)
