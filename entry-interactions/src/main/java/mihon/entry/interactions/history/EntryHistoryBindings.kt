package mihon.entry.interactions.history

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import tachiyomi.domain.history.repository.HistoryRepository

@ContributesTo(AppScope::class)
@BindingContainer
object EntryHistoryBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun feature(repository: HistoryRepository): EntryHistoryFeature = DefaultEntryHistoryFeature(repository)
}
