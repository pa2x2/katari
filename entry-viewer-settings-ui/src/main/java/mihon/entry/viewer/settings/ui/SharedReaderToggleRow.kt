package mihon.entry.viewer.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import mihon.entry.viewer.settings.ViewerSettingBinding
import mihon.entry.viewer.settings.shared.ReaderSharedSettingAvailability
import mihon.entry.viewer.settings.shared.ResolvedReaderSharedToggleSetting
import tachiyomi.presentation.core.components.CheckboxItem

@Composable
internal fun SharedReaderToggleRow(
    setting: ResolvedReaderSharedToggleSetting,
    binding: ViewerSettingBinding<Boolean>,
) {
    val context = LocalContext.current
    val resolved by binding.state.collectAsState()
    val checked = resolved.effectiveValue
    val profileDefault = resolved.profileValue ?: resolved.processorDefault
    val scope = rememberCoroutineScope()
    val availability = rememberReaderSharedSettingAvailability(setting)
    val enabled = availability == ReaderSharedSettingAvailability.Available
    val disabledAction = (availability as? ReaderSharedSettingAvailability.Disabled)?.action
    CheckboxItem(
        label = setting.title.resolve(context),
        subtitle = when (availability) {
            is ReaderSharedSettingAvailability.Disabled -> listOfNotNull(
                availability.reason.resolve(context),
                availability.action?.label?.resolve(context),
            ).joinToString(separator = "\n")
            null,
            ReaderSharedSettingAvailability.Available,
            -> setting.summary.resolve(context)
        },
        checked = checked,
        enabled = enabled,
        onClick = {
            scope.launch {
                val target = !checked
                if (target == profileDefault) {
                    binding.clearEntryOverride()
                } else {
                    binding.setEntryOverride(target)
                }
            }
        },
        onDisabledClick = disabledAction?.let { action ->
            { action.perform(context) }
        },
    )
}
