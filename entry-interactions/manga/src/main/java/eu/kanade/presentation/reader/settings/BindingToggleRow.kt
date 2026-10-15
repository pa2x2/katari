package eu.kanade.presentation.reader.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import mihon.entry.viewer.settings.ViewerSettingBinding
import mihon.entry.viewer.settings.updateEntry
import mihon.presentation.kit.list.ToggleRow

/** In-reader toggle that writes this series' override instead of the profile value. */
@Composable
internal fun BindingToggleRow(
    label: String,
    binding: ViewerSettingBinding<Boolean>,
) {
    val state by binding.state.collectAsState()
    val scope = rememberCoroutineScope()
    ToggleRow(
        title = label,
        checked = state.effectiveValue,
        onCheckedChange = { scope.launch { binding.updateEntry(it) } },
    )
}
