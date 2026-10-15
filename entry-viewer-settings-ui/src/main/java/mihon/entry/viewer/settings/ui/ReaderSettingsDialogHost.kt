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
import mihon.presentation.kit.feedback.SnackbarHostState
import mihon.presentation.kit.menu.DropdownMenuItem
import mihon.presentation.kit.sheet.TabbedSheet
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Common in-reader settings host. Reset is part of this host so a reader cannot add a settings surface without also
 * defining how it returns to its defaults.
 *
 * Applicable shared settings are projected from [sharedSettingsRegistry] so a reader cannot omit them,
 * while processor-owned pages are contributed by the caller in [content]. [sharedSettingDetails] adds what the open
 * entry shows with a shared setting, and [snackbarHostState] shows messages about changes made while the dialog covers
 * the reader. Reset clears processor and shared entry overrides so an in-reader reset only drops this entry's overrides.
 */
@Composable
fun ReaderSettingsDialogHost(
    sharedSettingsRegistry: ReaderSharedSettingsRegistry,
    settingsSurfaceId: String,
    capabilities: Set<ReaderCapabilityId>,
    sharedSettingBindings: Map<ReaderSharedSettingId, ViewerSettingBinding<Boolean>>,
    sharedTabTitle: String,
    processorTabTitles: List<String>,
    onDismissRequest: () -> Unit,
    onOpenDefaultSettings: () -> Unit,
    onResetProcessorSettings: suspend () -> Unit,
    onProcessorPageChanged: ((Int) -> Unit)? = null,
    sharedSettingDetails: Map<ReaderSharedSettingId, ReaderSharedSettingDetails> = emptyMap(),
    snackbarHostState: SnackbarHostState? = null,
    content: @Composable ColumnScope.(processorPage: Int) -> Unit,
) {
    val sharedSettings = remember(sharedSettingsRegistry, capabilities, settingsSurfaceId) {
        sharedSettingsRegistry.settingsFor(capabilities, settingsSurfaceId)
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

    TabbedSheet(
        onDismissRequest = onDismissRequest,
        tabTitles = tabTitles,
        pagerState = pagerState,
        menu = { close ->
            DropdownMenuItem(
                label = stringResource(MR.strings.action_open_default_reader_settings),
                onClick = {
                    close()
                    onOpenDefaultSettings()
                },
            )
            DropdownMenuItem(
                label = stringResource(MR.strings.action_reset),
                onClick = {
                    close()
                    scope.launch {
                        onResetProcessorSettings()
                        sharedSettings.forEach { setting ->
                            sharedSettingBindings.getValue(setting.id).clearEntryOverride()
                        }
                    }
                },
            )
        },
        snackbarHostState = snackbarHostState,
    ) { page ->
        if (page < processorTabOffset) {
            sharedSettings.forEach { setting ->
                val details = sharedSettingDetails[setting.id]
                SharedReaderToggleRow(setting, sharedSettingBindings.getValue(setting.id), details?.summary)
                details?.content?.invoke(this)
            }
        } else {
            content(page - processorTabOffset)
        }
    }
}
