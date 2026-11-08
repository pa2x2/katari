package app.katari.desktop.viewer

import androidx.compose.runtime.Composable
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.screen.animeplayer.PlayerEngineAddon

/** Desktop has no player engine installed apart from Katari, so the player settings have no add-on group. */
@Inject
class NoPlayerEngineAddon : PlayerEngineAddon {
    override val isOffered: Boolean = false

    @Composable
    override fun SettingsRow() {
        error("Desktop's player has no add-on")
    }
}
