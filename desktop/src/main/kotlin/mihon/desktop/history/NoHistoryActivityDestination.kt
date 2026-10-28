package mihon.desktop.history

import dev.zacsweers.metro.Inject
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.history.activity.HistoryActivityDestination
import java.time.LocalDate

/**
 * Desktop can't show the sessions in history yet, so days and bars in statistics don't open them. Stands in until
 * desktop hosts the history screens.
 */
@Inject
class NoHistoryActivityDestination : HistoryActivityDestination {
    override val isAvailable: Boolean = false

    override fun screen(start: LocalDate, end: LocalDate, type: EntryType?): Screen =
        error("History sessions are not available on desktop")
}
