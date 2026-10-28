package eu.kanade.presentation.more.settings.screen.appearance

import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.more.settings.screen.navigation.HomeNavigationEditorDestination
import mihon.core.common.CustomPreferences

/** Graph entry points of the appearance settings. */
interface AppearanceSettingsGraph {
    val uiPreferences: UiPreferences
    val customPreferences: CustomPreferences
    val appThemeHost: AppThemeHost
    val appLanguageDestination: AppLanguageDestination
    val homeNavigationEditorDestination: HomeNavigationEditorDestination
}
