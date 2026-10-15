package eu.kanade.presentation.more.settings.screen.navigation

import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.ui.home.navigation.homeNavigationTitle
import mihon.core.common.homeScreenContentTabOrder
import mihon.presentation.kit.list.SelectRow
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Picks the tab the app opens on, from the content tabs the draft keeps visible. */
@Composable
internal fun StartupTabSelector(
    draft: HomeNavigationEditorDraft,
    onDraftChange: (HomeNavigationEditorDraft) -> Unit,
) {
    val availableTabs = draft.configuration.tabOrder.filter { tab ->
        tab in draft.configuration.enabledTabs && tab in homeScreenContentTabOrder
    }
    SelectRow(
        label = stringResource(MR.strings.home_navigation_startup_tab),
        options = availableTabs.map { homeNavigationTitle(it) },
        selectedIndex = availableTabs.indexOf(draft.startupTab),
        onSelect = { index -> onDraftChange(draft.copy(startupTab = availableTabs[index])) },
    )
}
