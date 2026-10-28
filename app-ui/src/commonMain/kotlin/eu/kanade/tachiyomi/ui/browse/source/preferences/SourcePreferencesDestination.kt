package eu.kanade.tachiyomi.ui.browse.source.preferences

import eu.kanade.presentation.util.Screen

/** A source's own settings, which its extension builds, opened from its catalogue. */
interface SourcePreferencesDestination {
    /**
     * False where the app can't show [sourceId]'s settings, such as for a source that has none or a platform that
     * doesn't host extension settings; what would open them is then hidden.
     */
    fun isAvailable(sourceId: Long): Boolean

    /** Only called while [isAvailable]. */
    fun screen(sourceId: Long): Screen
}
