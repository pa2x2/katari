package eu.kanade.tachiyomi.ui.history.activity

import dev.zacsweers.metro.Inject
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.source.entry.EntryType
import java.time.LocalDate

@Inject
class HistoryActivityScreenDestination : HistoryActivityDestination {
    override val isAvailable: Boolean = true

    override fun screen(start: LocalDate, end: LocalDate, type: EntryType?): Screen = HistoryActivityScreen(
        startLocalDate = start.toString(),
        endLocalDate = end.toString(),
        typeName = type?.name,
    )
}
