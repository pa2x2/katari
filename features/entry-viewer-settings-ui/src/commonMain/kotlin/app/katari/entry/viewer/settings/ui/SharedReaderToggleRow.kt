package app.katari.entry.viewer.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import app.katari.core.platform.currentPlatformContext
import app.katari.entry.viewer.settings.ViewerSettingBinding
import app.katari.entry.viewer.settings.shared.ReaderSharedSettingAvailability
import app.katari.entry.viewer.settings.shared.ResolvedReaderSharedToggleSetting
import app.katari.presentation.kit.list.ToggleRow
import kotlinx.coroutines.launch

@Composable
internal fun SharedReaderToggleRow(
    setting: ResolvedReaderSharedToggleSetting,
    binding: ViewerSettingBinding<Boolean>,
    summary: String?,
) {
    val context = currentPlatformContext()
    val resolved by binding.state.collectAsState()
    val checked = resolved.effectiveValue
    val profileDefault = resolved.profileValue ?: resolved.processorDefault
    val scope = rememberCoroutineScope()
    val availability = rememberReaderSharedSettingAvailability(setting)
    val enabled = availability == ReaderSharedSettingAvailability.Available
    val disabledAction = (availability as? ReaderSharedSettingAvailability.Disabled)?.action
    ToggleRow(
        title = setting.title.resolve(context),
        subtitle = when (availability) {
            is ReaderSharedSettingAvailability.Disabled -> listOfNotNull(
                availability.reason.resolve(context),
                availability.action?.label?.resolve(context),
            ).joinToString(separator = "\n")
            null,
            ReaderSharedSettingAvailability.Available,
            -> summary ?: setting.summary.resolve(context)
        },
        checked = checked,
        enabled = enabled,
        onCheckedChange = { target ->
            scope.launch {
                if (target == profileDefault) {
                    binding.clearEntryOverride()
                } else {
                    binding.setEntryOverride(target)
                }
            }
        },
        setting = binding.definition.profilePreference,
        onDisabledClick = disabledAction?.let { action ->
            { action.perform(context) }
        },
    )
}
