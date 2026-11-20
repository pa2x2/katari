package app.katari.desktop.viewer

import androidx.compose.runtime.Composable
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.animeplayer.PlayerEngineAddon

/** Desktop has no player engine installed apart from Katari, so the player settings have no add-on group. */
@Inject
class NoPlayerEngineAddon : PlayerEngineAddon {
    @Composable
    override fun settingsItems(): List<Preference.PreferenceItem<out Any, out Any>> = emptyList()
}
