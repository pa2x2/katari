package mihon.desktop.browse

import dev.zacsweers.metro.Inject
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.browse.source.preferences.SourcePreferencesDestination

/**
 * Sources build their settings as Android's preference screens, which desktop can't show yet, so a catalogue doesn't
 * offer its source's settings. Stands in until desktop shows them.
 */
@Inject
class NoSourcePreferencesDestination : SourcePreferencesDestination {
    override fun isAvailable(sourceId: Long): Boolean = false

    override fun screen(sourceId: Long): Screen = error("Source settings are not available on desktop")
}
