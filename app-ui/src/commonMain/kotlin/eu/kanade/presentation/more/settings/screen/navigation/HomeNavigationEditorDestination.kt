package eu.kanade.presentation.more.settings.screen.navigation

import eu.kanade.presentation.util.Screen

/** The screen that arranges the home screen's tabs, opened from the appearance settings. */
interface HomeNavigationEditorDestination {
    /** False where the editor can't be shown yet; the setting is then hidden. */
    val isAvailable: Boolean

    /** Only called while [isAvailable]. */
    fun screen(): Screen
}
