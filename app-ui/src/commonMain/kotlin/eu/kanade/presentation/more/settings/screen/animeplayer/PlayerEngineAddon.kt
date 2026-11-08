package eu.kanade.presentation.more.settings.screen.animeplayer

import androidx.compose.runtime.Composable
import app.katari.core.common.platform.PlatformFacility

/**
 * The player engine the platform installs apart from Katari, which the player settings manage in a group of their own.
 * It is installed once for the device, so the group carries no profile chip.
 */
@PlatformFacility
interface PlayerEngineAddon {
    /** False where the player needs nothing installed apart from Katari; the group is then hidden. */
    val isOffered: Boolean

    /** The add-on's row, through its install, update and restart. Only composed while [isOffered]. */
    @Composable
    fun SettingsRow()
}
