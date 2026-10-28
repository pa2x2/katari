package mihon.desktop.entry

import dev.zacsweers.metro.Inject
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.entry.EntryDetailsDestination

/**
 * Desktop can't show an entry's details yet, so titles in shared screens, such as statistics, don't open. Stands in
 * until desktop hosts the entry screens.
 */
@Inject
class NoEntryDetailsDestination : EntryDetailsDestination {
    override val isAvailable: Boolean = false

    override fun screen(entryId: Long): Screen = error("Entry details are not available on desktop")
}
