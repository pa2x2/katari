package eu.kanade.presentation.reader.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import mihon.entry.viewer.settings.ViewerSettingBinding
import mihon.entry.viewer.settings.updateEntry
import tachiyomi.presentation.core.components.CheckboxItem

/** In-reader checkbox that writes this series' override instead of the profile value. */
@Composable
internal fun BindingCheckboxItem(
    label: String,
    binding: ViewerSettingBinding<Boolean>,
) {
    val state by binding.state.collectAsState()
    val scope = rememberCoroutineScope()
    CheckboxItem(
        label = label,
        checked = state.effectiveValue,
        onClick = { scope.launch { binding.updateEntry(!state.effectiveValue) } },
    )
}
