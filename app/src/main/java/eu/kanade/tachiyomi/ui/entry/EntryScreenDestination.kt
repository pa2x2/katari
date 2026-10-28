package eu.kanade.tachiyomi.ui.entry

import dev.zacsweers.metro.Inject
import eu.kanade.presentation.util.Screen

@Inject
class EntryScreenDestination : EntryDetailsDestination {
    override val isAvailable: Boolean = true

    override fun screen(entryId: Long): Screen = EntryScreen(entryId)
}
