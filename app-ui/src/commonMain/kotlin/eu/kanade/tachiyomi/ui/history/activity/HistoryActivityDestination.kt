package eu.kanade.tachiyomi.ui.history.activity

import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.source.entry.EntryType
import java.time.LocalDate

/** The screen a stretch of days opens to from shared screens, such as a bar in statistics: its sessions in history. */
interface HistoryActivityDestination {
    /** False where the history screens can't be shown yet; what would open the sessions is then not clickable. */
    val isAvailable: Boolean

    /**
     * The sessions from [start] to [end], both included, of every type or only [type]. Only called while [isAvailable].
     */
    fun screen(start: LocalDate, end: LocalDate, type: EntryType?): Screen
}

/** Opens the sessions of a stretch of days on [navigator]; null where they aren't available. */
fun HistoryActivityDestination.opener(navigator: Navigator): ((LocalDate, LocalDate, EntryType?) -> Unit)? =
    if (isAvailable) { start, end, type -> navigator.push(screen(start, end, type)) } else null
