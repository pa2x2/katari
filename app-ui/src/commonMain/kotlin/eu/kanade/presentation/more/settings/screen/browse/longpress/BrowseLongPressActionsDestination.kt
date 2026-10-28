package eu.kanade.presentation.more.settings.screen.browse.longpress

import eu.kanade.presentation.util.Screen

/** The screen that orders what a long press on a browsed entry does, by default and for single sources. */
interface BrowseLongPressActionsDestination {
    /** False where browsing sources can't be shown yet; the setting is then hidden. */
    val isAvailable: Boolean

    /** Only called while [isAvailable]. */
    fun screen(): Screen
}
