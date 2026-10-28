package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.tachiyomi.ui.entry.EntryDetailsDestination
import eu.kanade.tachiyomi.ui.history.activity.HistoryActivityDestination
import mihon.desktop.entry.NoEntryDetailsDestination
import mihon.desktop.history.NoHistoryActivityDestination

@BindingContainer
interface DestinationBindings {
    @Binds
    val NoEntryDetailsDestination.entryDetailsDestination: EntryDetailsDestination

    @Binds
    val NoHistoryActivityDestination.historyActivityDestination: HistoryActivityDestination
}
