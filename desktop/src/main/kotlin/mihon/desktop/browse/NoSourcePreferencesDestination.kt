package mihon.desktop.browse

import dev.zacsweers.metro.Inject
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.browse.source.preferences.SourcePreferencesDestination

/**
 * Extensions build their settings from Android's preference screens, which desktop can't show, so a catalogue doesn't
 * offer its source's settings. Stands in until desktop hosts extensions with settings of their own.
 */
@Inject
class NoSourcePreferencesDestination : SourcePreferencesDestination {
    override fun isAvailable(sourceId: Long): Boolean = false

    override fun screen(sourceId: Long): Screen = error("Source settings are not available on desktop")
}
