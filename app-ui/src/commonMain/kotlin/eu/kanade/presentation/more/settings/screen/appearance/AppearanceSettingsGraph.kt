package eu.kanade.presentation.more.settings.screen.appearance

import app.katari.core.common.CustomPreferences
import eu.kanade.domain.ui.UiPreferences

/** Graph entry points of the appearance settings. */
interface AppearanceSettingsGraph {
    val uiPreferences: UiPreferences
    val customPreferences: CustomPreferences
    val appThemeHost: AppThemeHost
    val interfaceSizeSetting: InterfaceSizeSetting
}
