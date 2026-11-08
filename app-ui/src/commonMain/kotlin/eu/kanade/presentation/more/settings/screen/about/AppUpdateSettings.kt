package eu.kanade.presentation.more.settings.screen.about

import androidx.compose.runtime.Composable
import app.katari.core.common.platform.PlatformFacility

/** The app updating itself, which About checks for and configures. */
@PlatformFacility
interface AppUpdateSettings {
    /**
     * About's update rows: the check with the update's state, and the settings that shape it. Empty where the build
     * can't update itself.
     */
    fun aboutRows(): List<@Composable () -> Unit>
}
