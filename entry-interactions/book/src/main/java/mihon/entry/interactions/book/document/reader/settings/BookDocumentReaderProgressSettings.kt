package mihon.entry.interactions.book.document.reader.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mihon.entry.viewer.settings.ViewerSettingBinding
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.RadioItem
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun BookDocumentReaderProgressSettings(
    showProgressBinding: ViewerSettingBinding<Boolean>,
    styleBinding: ViewerSettingBinding<BookDocumentReaderProgressStyle>,
) {
    val showProgressSetting by showProgressBinding.state.collectAsState()
    val styleSetting by styleBinding.state.collectAsState()
    val scope = rememberCoroutineScope()
    val showProgressProfileDefault = showProgressSetting.profileValue ?: showProgressSetting.processorDefault
    val styleProfileDefault = styleSetting.profileValue ?: styleSetting.processorDefault

    CheckboxItem(
        label = stringResource(MR.strings.pref_book_document_reader_show_reading_progress),
        checked = showProgressSetting.effectiveValue,
        onClick = {
            scope.launch {
                val target = !showProgressSetting.effectiveValue
                if (target == showProgressProfileDefault) {
                    showProgressBinding.clearEntryOverride()
                } else {
                    showProgressBinding.setEntryOverride(target)
                }
            }
        },
    )
    AnimatedVisibility(visible = showProgressSetting.effectiveValue) {
        Column {
            Text(
                text = stringResource(MR.strings.pref_book_document_reader_reading_progress_style),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            BookDocumentReaderProgressStyle.entries.forEach { style ->
                RadioItem(
                    label = style.label(),
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
