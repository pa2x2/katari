package eu.kanade.presentation.more.settings.screen.general

import app.katari.core.platform.locale.AppLocaleStore
import eu.kanade.domain.ui.UiPreferences

/** Graph entry points of the general settings. */
interface GeneralSettingsGraph {
    val uiPreferences: UiPreferences
    val appLocaleStore: AppLocaleStore
    val backgroundActivitySettings: BackgroundActivitySettings
}
