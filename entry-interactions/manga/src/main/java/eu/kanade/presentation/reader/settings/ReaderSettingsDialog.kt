package eu.kanade.presentation.reader.settings

import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsScreenModel
import mihon.entry.interactions.reader.settings.MangaReaderSettings
import mihon.entry.viewer.settings.ui.ReaderSettingsDialogHost
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

private const val COLOR_FILTER_PAGE = 2

@Composable
internal fun ReaderSettingsDialog(
    onDismissRequest: () -> Unit,
    onOpenDefaultSettings: () -> Unit,
    onShowMenus: () -> Unit,
    onHideMenus: () -> Unit,
    screenModel: ReaderSettingsScreenModel,
) {
    val tabTitles = listOf(
        stringResource(MR.strings.pref_category_reading_mode),
        stringResource(MR.strings.pref_category_general),
        stringResource(MR.strings.custom_filter),
    )
    var dialogWindow by remember { mutableStateOf<Window?>(null) }

    ReaderSettingsDialogHost(
        settingsSurfaceId = MangaReaderSettings.PROVIDER_ID,
        capabilities = MangaReaderSettings.READER_CAPABILITIES,
        sharedSettingBindings = screenModel.settings.sharedSettings,
        sharedTabTitle = stringResource(MR.strings.reader_shared_settings),
        processorTabTitles = tabTitles,
        onDismissRequest = {
            onDismissRequest()
            onShowMenus()
        },
        onOpenDefaultSettings = {
            onDismissRequest()
            onShowMenus()
            onOpenDefaultSettings()
        },
        onResetProcessorSettings = screenModel::clearEntryOverrides,
        onProcessorPageChanged = { page ->
            val window = dialogWindow ?: return@ReaderSettingsDialogHost
            if (page == COLOR_FILTER_PAGE) {
                window.setDimAmount(0f)
                onHideMenus()
            } else {
                window.setDimAmount(0.5f)
                onShowMenus()
            }
        },
    ) { page ->
        val view = LocalView.current
        SideEffect {
            dialogWindow = (view.parent as? DialogWindowProvider)?.window
        }
        when (page) {
            0 -> ReadingModePage(screenModel)
            1 -> GeneralPage(screenModel)
            2 -> ColorFilterPage(screenModel)
        }
    }
}
