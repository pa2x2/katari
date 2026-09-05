package mihon.entry.interactions.book.document.reader.settings

import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import mihon.entry.interactions.reader.settings.BookDocumentReadingMode
import mihon.entry.viewer.settings.ViewerSettingBinding
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.SettingsChipRow
import tachiyomi.presentation.core.components.reader.navigation.ReaderTapZoneSettings
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun BookDocumentReaderPagingSettings(bindings: BookDocumentReaderSettingBindings) {
    val mode by bindings.readingMode.state.collectAsState()
    val zones by bindings.tapZones.state.collectAsState()
    val inversion by bindings.tapInversion.state.collectAsState()
    val scope = rememberCoroutineScope()
    SettingsChipRow(MR.strings.pref_category_reading_mode) {
        BookDocumentReadingMode.entries.forEach { value ->
            FilterChip(
                mode.effectiveValue == value,
                { scope.launch { bindings.readingMode.updateEntry(value) } },
                label = { Text(stringResource(value.stringRes)) },
            )
        }
    }
    if (mode.effectiveValue != BookDocumentReadingMode.SCROLL) {
        ReaderTapZoneSettings(
            zones.effectiveValue,
            { scope.launch { bindings.tapZones.updateEntry(it) } },
            inversion.effectiveValue,
            { scope.launch { bindings.tapInversion.updateEntry(it) } },
        )
        PagingCheckbox(bindings.animatePages, stringResource(MR.strings.pref_page_transitions))
        PagingCheckbox(bindings.volumeKeys, stringResource(MR.strings.pref_read_with_volume_keys))
        PagingCheckbox(bindings.invertVolumeKeys, stringResource(MR.strings.pref_read_with_volume_keys_inverted))
    }
}

@Composable
private fun PagingCheckbox(binding: ViewerSettingBinding<Boolean>, label: String) {
    val state by binding.state.collectAsState()
    val scope = rememberCoroutineScope()
    CheckboxItem(label = label, checked = state.effectiveValue, onClick = {
        scope.launch { binding.updateEntry(!state.effectiveValue) }
    })
}

private suspend fun <T : Any> ViewerSettingBinding<T>.updateEntry(value: T) {
    val current = state.value
    if (value == (current.profileValue ?: current.processorDefault)) clearEntryOverride() else setEntryOverride(value)
}
