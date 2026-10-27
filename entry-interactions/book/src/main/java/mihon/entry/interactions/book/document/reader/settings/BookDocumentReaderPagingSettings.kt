package mihon.entry.interactions.book.document.reader.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import mihon.entry.interactions.reader.settings.BookDocumentPageLayout
import mihon.entry.interactions.reader.settings.BookDocumentReadingMode
import mihon.entry.viewer.chrome.ui.navigation.ReaderTapZoneSettings
import mihon.entry.viewer.settings.ViewerSettingBinding
import mihon.entry.viewer.settings.updateEntry
import mihon.presentation.kit.chip.FilterChip
import mihon.presentation.kit.list.ChipsRow
import mihon.presentation.kit.list.RowGroup
import mihon.presentation.kit.list.ToggleRow
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun BookDocumentReaderPagingSettings(bindings: BookDocumentReaderSettingBindings) {
    val mode by bindings.readingMode.state.collectAsState()
    val pageLayout by bindings.pageLayout.state.collectAsState()
    val zones by bindings.tapZones.state.collectAsState()
    val inversion by bindings.tapInversion.state.collectAsState()
    val scope = rememberCoroutineScope()
    ChipsRow(title = stringResource(MR.strings.pref_category_reading_mode)) {
        BookDocumentReadingMode.entries.forEach { value ->
            FilterChip(
                label = stringResource(value.stringRes),
                selected = mode.effectiveValue == value,
                onClick = { scope.launch { bindings.readingMode.updateEntry(value) } },
            )
        }
    }
    if (mode.effectiveValue.isHorizontallyPaged) {
        ChipsRow(title = stringResource(MR.strings.pref_book_document_reader_pages)) {
            BookDocumentPageLayout.entries.forEach { value ->
                FilterChip(
                    label = stringResource(value.stringRes),
                    selected = pageLayout.effectiveValue == value,
                    onClick = { scope.launch { bindings.pageLayout.updateEntry(value) } },
                )
            }
        }
    }
    if (mode.effectiveValue != BookDocumentReadingMode.SCROLL) {
        ReaderTapZoneSettings(
            zones.effectiveValue,
            { scope.launch { bindings.tapZones.updateEntry(it) } },
            inversion.effectiveValue,
            { scope.launch { bindings.tapInversion.updateEntry(it) } },
        )
        RowGroup {
            row { PagingToggle(bindings.animatePages, stringResource(MR.strings.pref_page_transitions)) }
            row { PagingToggle(bindings.volumeKeys, stringResource(MR.strings.pref_read_with_volume_keys)) }
            row {
                PagingToggle(bindings.invertVolumeKeys, stringResource(MR.strings.pref_read_with_volume_keys_inverted))
            }
        }
    }
}

@Composable
private fun PagingToggle(binding: ViewerSettingBinding<Boolean>, label: String) {
    val state by binding.state.collectAsState()
    val scope = rememberCoroutineScope()
    ToggleRow(
        title = label,
        checked = state.effectiveValue,
        onCheckedChange = { scope.launch { binding.updateEntry(it) } },
    )
}
