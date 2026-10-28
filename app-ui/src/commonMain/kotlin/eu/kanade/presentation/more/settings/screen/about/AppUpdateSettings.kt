package eu.kanade.presentation.more.settings.screen.about

import androidx.compose.runtime.Composable

/** The app updating itself, which About checks for and configures. */
interface AppUpdateSettings {
    /**
     * About's update rows: the check with the update's state, and the settings that shape it. Empty where the build
     * can't update itself.
     */
    fun aboutRows(): List<@Composable () -> Unit>
}
