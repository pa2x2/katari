package eu.kanade.presentation.more.settings.screen.navigation

import androidx.compose.runtime.Composable
import eu.kanade.presentation.more.settings.widget.ListPreferenceWidget
import eu.kanade.tachiyomi.ui.home.navigation.homeNavigationTitle
import mihon.core.common.homeScreenContentTabOrder
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
    val titles = availableTabs.associateWith { homeNavigationTitle(it) }
    ListPreferenceWidget(
        value = draft.startupTab,
        title = stringResource(MR.strings.home_navigation_startup_tab),
        subtitle = titles[draft.startupTab],
        icon = null,
        isProfileSpecific = false,
        entries = titles,
        onValueChange = { onDraftChange(draft.copy(startupTab = it)) },
    )
}
