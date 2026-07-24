package eu.kanade.presentation.reader.settings

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import mihon.entry.interactions.reader.settings.ReaderSettingsScreenModel
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.ViewerSettingsTabbedDialog
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun ReaderSettingsDialog(
    onDismissRequest: () -> Unit,
    onShowMenus: () -> Unit,
    onHideMenus: () -> Unit,
    screenModel: ReaderSettingsScreenModel,
) {
    val tabTitles = listOf(
        stringResource(MR.strings.pref_category_reading_mode),
        stringResource(MR.strings.pref_category_general),
        stringResource(MR.strings.custom_filter),
    )
    val pagerState = rememberPagerState { tabTitles.size }

    ViewerSettingsTabbedDialog(
        onDismissRequest = {
            onDismissRequest()
            onShowMenus()
        },
        onResetSettings = screenModel::resetSettings,
        tabTitles = tabTitles,
        pagerState = pagerState,
    ) { page ->
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window

        LaunchedEffect(pagerState.currentPage) {
            if (pagerState.currentPage == 2) {
                window?.setDimAmount(0f)
                onHideMenus()
            } else {
                window?.setDimAmount(0.5f)
                onShowMenus()
            }
        }

        when (page) {
            0 -> ReadingModePage(screenModel)
            1 -> GeneralPage(screenModel)
            2 -> ColorFilterPage(screenModel)
        }
    }
}
