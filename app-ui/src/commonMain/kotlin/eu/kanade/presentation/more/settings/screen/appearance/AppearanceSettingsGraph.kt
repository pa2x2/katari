package eu.kanade.presentation.more.settings.screen.appearance

import eu.kanade.domain.ui.UiPreferences
import mihon.core.common.CustomPreferences
import mihon.core.platform.locale.AppLocaleStore

/** Graph entry points of the appearance settings. */
interface AppearanceSettingsGraph {
    val uiPreferences: UiPreferences
    val customPreferences: CustomPreferences
    val appThemeHost: AppThemeHost
    val appLocaleStore: AppLocaleStore
    val interfaceSizeSetting: InterfaceSizeSetting
}
