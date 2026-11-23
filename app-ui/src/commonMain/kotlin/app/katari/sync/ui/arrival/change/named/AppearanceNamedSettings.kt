package app.katari.sync.ui.arrival.change.named

import app.katari.core.common.CustomPreferences
import app.katari.core.common.toHomeScreenTabs
import app.katari.sync.ui.arrival.change.setChanges
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.more.settings.widget.themeModeLabels
import eu.kanade.tachiyomi.ui.home.navigation.homeNavigationTitle
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Appearance's settings changed on rows of their own make: the theme's mode, and the navigation editor's tabs. */
internal val appearanceNamedSettings = listOf(
    valueSetting(MR.strings.pref_category_theme, { UiPreferences(it).themeMode }) {
        stringResource(themeModeLabels.getValue(it))
    },
    valueSetting(MR.strings.home_navigation_startup_tab, { CustomPreferences(it).homeScreenStartupTab }) {
        homeNavigationTitle(it)
    },
    namedSetting(MR.strings.pref_home_screen_tabs, { CustomPreferences(it).homeScreenTabs }) { name, value, _ ->
        setChanges(name, value.before.toHomeScreenTabs(), value.after.toHomeScreenTabs(), { homeNavigationTitle(it) })
    },
    orderSetting(MR.strings.sync_setting_navigation_order, { CustomPreferences(it).homeScreenTabOrder }) {
        homeNavigationTitle(it)
    },
    orderSetting(MR.strings.sync_setting_navigation_bar, { CustomPreferences(it).homeScreenPrimaryTabs }) {
        homeNavigationTitle(it)
    },
)
