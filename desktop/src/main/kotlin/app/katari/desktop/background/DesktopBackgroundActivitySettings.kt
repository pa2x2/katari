package app.katari.desktop.background

import androidx.compose.runtime.Composable
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.general.BackgroundActivitySettings

/** Desktop systems don't restrict an app's background work the way Android's battery optimisation does. */
@Inject
class DesktopBackgroundActivitySettings : BackgroundActivitySettings {
    @Composable
    override fun preferences(): List<Preference.PreferenceItem<out Any, out Any>> = emptyList()
}
