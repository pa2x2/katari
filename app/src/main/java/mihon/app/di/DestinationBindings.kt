package mihon.app.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.tachiyomi.ui.entry.EntryDetailsDestination
import eu.kanade.tachiyomi.ui.entry.EntryScreenDestination
import eu.kanade.tachiyomi.ui.history.activity.HistoryActivityDestination
import eu.kanade.tachiyomi.ui.history.activity.HistoryActivityScreenDestination

@BindingContainer
interface DestinationBindings {
    @Binds
    val EntryScreenDestination.entryDetailsDestination: EntryDetailsDestination

    @Binds
    val HistoryActivityScreenDestination.historyActivityDestination: HistoryActivityDestination
}
