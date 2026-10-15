package mihon.entry.interactions.statistics

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.runtime.EntryInteractions
import mihon.entry.interactions.runtime.EntryStatisticsProvider
import mihon.entry.interactions.runtime.EntryTypePresentationProvider

@ContributesTo(AppScope::class)
@BindingContainer
object EntryStatisticsBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun feature(
        statisticsProviders: Map<EntryType, EntryStatisticsProvider>,
        typePresentationProviders: Map<EntryType, EntryTypePresentationProvider>,
        interactions: EntryInteractions,
    ): EntryStatisticsFeature = DefaultEntryStatisticsFeature(
        applicableTypes = statisticsProviders.keys intersect typePresentationProviders.keys,
        interaction = interactions.statistics,
    )
}
