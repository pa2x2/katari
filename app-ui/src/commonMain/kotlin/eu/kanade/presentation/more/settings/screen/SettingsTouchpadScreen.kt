package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import app.katari.platform.presentation.platformPresentation
import app.katari.ui.di.AppUiGraph
import app.katari.ui.di.LocalAppUiGraph
import eu.kanade.presentation.more.settings.Preference

/** How a touchpad scrolls the app, on a platform that reads touchpads itself rather than as touches. */
class SettingsTouchpadScreen : SearchableSettings() {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = requireNotNull(platformPresentation.touchpadSettings).title

    override fun isOffered(graph: AppUiGraph): Boolean = platformPresentation.touchpadSettings != null

    @Composable
    override fun getPreferences(): List<Preference> = LocalAppUiGraph.current.touchpadSettings.preferences()
}
