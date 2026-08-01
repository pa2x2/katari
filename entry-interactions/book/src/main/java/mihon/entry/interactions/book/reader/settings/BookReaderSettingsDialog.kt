package mihon.entry.interactions.book.reader.settings

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import mihon.entry.interactions.book.R
import mihon.entry.viewer.settings.ViewerSettingBinding
import mihon.entry.viewer.settings.shared.ReaderCapabilityId
import mihon.entry.viewer.settings.shared.ReaderSharedSettingAvailability
import mihon.entry.viewer.settings.shared.ReaderSharedSettingId
import mihon.entry.viewer.settings.shared.ReaderSharedSettingsRegistry
import mihon.entry.viewer.settings.shared.ResolvedReaderSharedToggleSetting
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.ViewerSettingsTabbedDialog
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Common BOOK settings host. Processor settings remain processor-owned while applicable shared settings are inserted
 * once for every capable reader.
 */
@Composable
internal fun BookReaderSettingsDialog(
    settingsSurfaceId: String,
    capabilities: Set<ReaderCapabilityId>,
    sharedSettingBindings: Map<ReaderSharedSettingId, ViewerSettingBinding<Boolean>>,
    onDismissRequest: () -> Unit,
    onOpenDefaultSettings: () -> Unit,
    onResetProcessorSettings: suspend () -> Unit,
    processorTabTitles: List<String>,
    content: @Composable ColumnScope.(Int) -> Unit,
) {
    val registry = remember { Injekt.get<ReaderSharedSettingsRegistry>() }
    val sharedSettings = remember(registry, capabilities, settingsSurfaceId) {
        registry.settingsFor(capabilities, settingsSurfaceId)
    }
    val tabTitles = if (sharedSettings.isEmpty()) {
        processorTabTitles
    } else {
        listOf(LocalContext.current.getString(R.string.book_reader_general_settings)) + processorTabTitles
    }
    val scope = rememberCoroutineScope()

    ViewerSettingsTabbedDialog(
        onDismissRequest = onDismissRequest,
        onResetSettings = {
            scope.launch {
                onResetProcessorSettings()
                sharedSettings.forEach { setting ->
                    sharedSettingBindings.getValue(setting.id).clearEntryOverride()
                }
            }
        },
        tabTitles = tabTitles,
        onOpenDefaultSettings = {
            onDismissRequest()
            onOpenDefaultSettings()
        },
        openDefaultSettingsLabel = stringResource(MR.strings.action_open_default_reader_settings),
    ) { page ->
        if (sharedSettings.isNotEmpty() && page == 0) {
            sharedSettings.forEach { setting ->
                SharedReaderToggleRow(setting, sharedSettingBindings.getValue(setting.id))
            }
        } else {
            content(page - if (sharedSettings.isEmpty()) 0 else 1)
        }
    }
}

@Composable
private fun SharedReaderToggleRow(
    setting: ResolvedReaderSharedToggleSetting,
    binding: ViewerSettingBinding<Boolean>,
) {
    val context = LocalContext.current
    val resolved by binding.state.collectAsState()
    val checked = resolved.effectiveValue
    val profileDefault = resolved.profileValue ?: resolved.processorDefault
    val scope = rememberCoroutineScope()
    val availability = rememberSharedSettingAvailability(setting)
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

@Composable
private fun rememberSharedSettingAvailability(
    setting: ResolvedReaderSharedToggleSetting,
): ReaderSharedSettingAvailability? {
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumeGeneration by remember(setting) { mutableIntStateOf(0) }
    var availability by remember(setting) { mutableStateOf<ReaderSharedSettingAvailability?>(null) }

    DisposableEffect(lifecycleOwner, setting) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeGeneration++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(setting, resumeGeneration) {
        availability = setting.resolveAvailability()
    }
    LaunchedEffect(setting) {
        setting.availabilityChanges.collect {
            availability = setting.resolveAvailability()
        }
    }
    return availability
}
