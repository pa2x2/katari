package mihon.entry.interactions.book.document.reader.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import mihon.entry.viewer.settings.ViewerSettingBinding
import mihon.presentation.kit.list.ChoiceRow
import mihon.presentation.kit.list.SectionHeader
import mihon.presentation.kit.list.ToggleRow
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun BookDocumentReaderProgressSettings(
    showProgressBinding: ViewerSettingBinding<Boolean>,
    styleBinding: ViewerSettingBinding<BookDocumentReaderProgressStyle>,
    showStyleOptions: Boolean = true,
) {
    val showProgressSetting by showProgressBinding.state.collectAsState()
    val styleSetting by styleBinding.state.collectAsState()
    val scope = rememberCoroutineScope()
    val showProgressProfileDefault = showProgressSetting.profileValue ?: showProgressSetting.processorDefault
    val styleProfileDefault = styleSetting.profileValue ?: styleSetting.processorDefault

    ToggleRow(
        title = stringResource(MR.strings.pref_book_document_reader_show_reading_progress),
        checked = showProgressSetting.effectiveValue,
        onCheckedChange = { target ->
            scope.launch {
                if (target == showProgressProfileDefault) {
                    showProgressBinding.clearEntryOverride()
                } else {
                    showProgressBinding.setEntryOverride(target)
                }
            }
        },
    )
    AnimatedVisibility(visible = showProgressSetting.effectiveValue && showStyleOptions) {
        Column {
            SectionHeader(text = stringResource(MR.strings.pref_book_document_reader_reading_progress_style))
            BookDocumentReaderProgressStyle.entries.forEach { style ->
                ChoiceRow(
                    title = style.label(),
                    selected = styleSetting.effectiveValue == style,
                    onClick = {
                        scope.launch {
                            if (style == styleProfileDefault) {
                                styleBinding.clearEntryOverride()
                            } else {
                                styleBinding.setEntryOverride(style)
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun BookDocumentReaderProgressStyle.label(): String = stringResource(
    when (this) {
        BookDocumentReaderProgressStyle.EDGE_FILL_RAIL ->
            MR.strings.book_document_reader_progress_edge_fill_rail
        BookDocumentReaderProgressStyle.EDGE_POSITION_MARKER ->
            MR.strings.book_document_reader_progress_edge_position_marker
        BookDocumentReaderProgressStyle.BOTTOM_HAIRLINE ->
            MR.strings.book_document_reader_progress_bottom_hairline
        BookDocumentReaderProgressStyle.PERCENTAGE ->
            MR.strings.book_document_reader_progress_percentage
    },
)
