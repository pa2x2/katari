package mihon.entry.interactions.book

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import mihon.entry.viewer.settings.ReaderCapabilityId
import mihon.entry.viewer.settings.ReaderSharedSettingAvailability
import mihon.entry.viewer.settings.ReaderSharedSettingsRegistry
import mihon.entry.viewer.settings.ResolvedReaderSharedToggleSetting
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.ViewerSettingsTabbedDialog
import tachiyomi.presentation.core.util.collectAsState
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
    onDismissRequest: () -> Unit,
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
                sharedSettings.forEach(ResolvedReaderSharedToggleSetting::reset)
            }
        },
        tabTitles = tabTitles,
    ) { page ->
        if (sharedSettings.isNotEmpty() && page == 0) {
            sharedSettings.forEach { setting -> SharedReaderToggleRow(setting) }
        } else {
            content(page - if (sharedSettings.isEmpty()) 0 else 1)
        }
    }
}

@Composable
private fun SharedReaderToggleRow(setting: ResolvedReaderSharedToggleSetting) {
    val context = LocalContext.current
    val checked by setting.preference.collectAsState()
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
            setting.preference.set(!checked)
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
