package mihon.entry.viewer.settings.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import mihon.entry.viewer.settings.ViewerSettingBinding
import mihon.entry.viewer.settings.shared.ReaderCapabilityId
import mihon.entry.viewer.settings.shared.ReaderSharedSettingId
import mihon.entry.viewer.settings.shared.ReaderSharedSettingsRegistry
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.ViewerSettingsTabbedDialog
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Common in-reader settings host.
 *
 * Applicable shared settings are projected from [ReaderSharedSettingsRegistry] so a reader cannot omit them,
 * while processor-owned pages are contributed by the caller in [content]. Reset clears processor and shared entry
 * overrides so an in-reader reset only drops this entry's overrides.
 */
@Composable
fun ReaderSettingsDialogHost(
    settingsSurfaceId: String,
    capabilities: Set<ReaderCapabilityId>,
    sharedSettingBindings: Map<ReaderSharedSettingId, ViewerSettingBinding<Boolean>>,
    sharedTabTitle: String,
    processorTabTitles: List<String>,
    onDismissRequest: () -> Unit,
    onOpenDefaultSettings: () -> Unit,
    onResetProcessorSettings: suspend () -> Unit,
    onProcessorPageChanged: ((Int) -> Unit)? = null,
    content: @Composable ColumnScope.(processorPage: Int) -> Unit,
) {
    val registry = remember { Injekt.get<ReaderSharedSettingsRegistry>() }
    val sharedSettings = remember(registry, capabilities, settingsSurfaceId) {
        registry.settingsFor(capabilities, settingsSurfaceId)
    }
    val processorTabOffset = if (sharedSettings.isEmpty()) 0 else 1
    val tabTitles = if (processorTabOffset == 0) {
        processorTabTitles
    } else {
        listOf(sharedTabTitle) + processorTabTitles
    }
    val pagerState = rememberPagerState { tabTitles.size }
    val scope = rememberCoroutineScope()

    if (onProcessorPageChanged != null) {
        LaunchedEffect(pagerState.currentPage, processorTabOffset) {
            onProcessorPageChanged(pagerState.currentPage - processorTabOffset)
        }
    }

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
        pagerState = pagerState,
        onOpenDefaultSettings = onOpenDefaultSettings,
        openDefaultSettingsLabel = stringResource(MR.strings.action_open_default_reader_settings),
    ) { page ->
        if (page < processorTabOffset) {
            sharedSettings.forEach { setting ->
                SharedReaderToggleRow(setting, sharedSettingBindings.getValue(setting.id))
            }
        } else {
            content(page - processorTabOffset)
        }
    }
}
