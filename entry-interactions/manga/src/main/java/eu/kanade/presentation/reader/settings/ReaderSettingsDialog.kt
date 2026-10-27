package eu.kanade.presentation.reader.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsScreenModel
import mihon.core.metro.metroGraph
import mihon.entry.interactions.reader.settings.MangaReaderSettings
import mihon.entry.interactions.runtime.EntryInteractionActivityGraph
import mihon.entry.viewer.settings.ui.ReaderSettingsDialogHost
import mihon.presentation.kit.sheet.SheetBackdrop
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
    val context = LocalContext.current
    val sharedSettingsRegistry = remember {
        context.metroGraph<EntryInteractionActivityGraph>().readerSharedSettingsRegistry
    }

    ReaderSettingsDialogHost(
        sharedSettingsRegistry = sharedSettingsRegistry,
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
            // The colour filter shows on the whole page, without the bars over it
            if (page == COLOR_FILTER_PAGE) onHideMenus() else onShowMenus()
        },
        // The page stays bright so a change of background, scale or crop shows as it is made
        backdrop = SheetBackdrop.Clear,
    ) { page ->
        when (page) {
            0 -> ReadingModePage(screenModel)
            1 -> GeneralPage(screenModel)
            2 -> ColorFilterPage(screenModel)
        }
    }
}
