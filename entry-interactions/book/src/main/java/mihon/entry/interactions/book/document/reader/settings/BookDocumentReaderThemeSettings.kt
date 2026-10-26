package mihon.entry.interactions.book.document.reader.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import mihon.entry.interactions.book.R
import mihon.entry.viewer.settings.ViewerSettingBinding
import mihon.presentation.kit.list.ChoiceRow
import mihon.presentation.kit.list.RowGroup
import mihon.presentation.kit.list.RowGroupHeader
import mihon.presentation.kit.list.ToggleRow

@Composable
internal fun BookDocumentReaderThemeSettings(
    binding: ViewerSettingBinding<BookDocumentReaderThemeMode>,
    publisherStylesBinding: ViewerSettingBinding<Boolean>,
) {
    val publisherStyles by publisherStylesBinding.state.collectAsState()
    val publisherStylesDefault = publisherStyles.profileValue ?: publisherStyles.processorDefault
    val setting by binding.state.collectAsState()
    val scope = rememberCoroutineScope()
    val profileDefault = setting.profileValue ?: setting.processorDefault
    RowGroupHeader(stringResource(R.string.book_document_reader_theme))
    RowGroup {
        BookDocumentReaderThemeMode.entries.forEach { mode ->
            row(key = mode) {
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
        row(key = PUBLISHER_STYLES_ROW) {
            ToggleRow(
                title = stringResource(R.string.book_document_reader_publisher_styles),
                subtitle = stringResource(R.string.book_document_reader_publisher_styles_summary),
                checked = publisherStyles.effectiveValue,
                onCheckedChange = { target ->
                    scope.launch {
                        if (target == publisherStylesDefault) {
                            publisherStylesBinding.clearEntryOverride()
                        } else {
                            publisherStylesBinding.setEntryOverride(target)
                        }
                    }
                },
            )
        }
    }
}

private const val PUBLISHER_STYLES_ROW = "publisher-styles"

@Composable
private fun BookDocumentReaderThemeMode.label(): String = stringResource(
    when (this) {
        BookDocumentReaderThemeMode.APP -> R.string.book_document_reader_theme_app
        BookDocumentReaderThemeMode.PAPER -> R.string.book_document_reader_theme_paper
        BookDocumentReaderThemeMode.DUSK -> R.string.book_document_reader_theme_dusk
        BookDocumentReaderThemeMode.BLACK -> R.string.book_document_reader_theme_black
    },
)
