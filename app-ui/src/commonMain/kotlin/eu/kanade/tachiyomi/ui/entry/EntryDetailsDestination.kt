package eu.kanade.tachiyomi.ui.entry

import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.presentation.util.Screen

/** The screen an entry opens to from shared screens, such as a title in statistics: the entry's details. */
interface EntryDetailsDestination {
    /** False where the entry screens can't be shown yet; what would open an entry is then not clickable. */
    val isAvailable: Boolean

    /** Only called while [isAvailable]. */
    fun screen(entryId: Long): Screen
}

/** Opens an entry's details on [navigator]; null where they aren't available. */
fun EntryDetailsDestination.opener(navigator: Navigator): ((Long) -> Unit)? =
    if (isAvailable) { entryId -> navigator.push(screen(entryId)) } else null
