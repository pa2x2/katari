package eu.kanade.presentation.more.settings.screen.animeplayer

import androidx.compose.runtime.Composable
import app.katari.core.common.platform.PlatformFacility
import eu.kanade.presentation.more.settings.Preference

/**
 * The player engine the platform installs apart from Katari, which the player settings manage in a group of their own.
 * It is installed once for the device, so the group carries no profile chip.
 */
@PlatformFacility
interface PlayerEngineAddon {
    /**
     * The add-on group's rows, through its install, update and restart. Empty where the player needs nothing
     * installed apart from Katari; the group is then hidden.
     */
    @Composable
    fun settingsItems(): List<Preference.PreferenceItem<out Any, out Any>>
}
