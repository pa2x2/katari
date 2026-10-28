package eu.kanade.presentation.more.settings.screen.appearance

import eu.kanade.presentation.util.Screen

/** The screen that gives the app a language other than the system's. */
interface AppLanguageDestination {
    /** False where the platform can't give the app a language of its own; the setting is then hidden. */
    val isAvailable: Boolean

    /** Only called while [isAvailable]. */
    fun screen(): Screen
}
