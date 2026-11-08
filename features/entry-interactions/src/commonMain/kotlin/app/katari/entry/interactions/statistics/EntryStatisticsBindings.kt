package app.katari.entry.interactions.statistics

import app.katari.entry.interactions.runtime.EntryInteractions
import app.katari.entry.interactions.runtime.EntryStatisticsProvider
import app.katari.entry.interactions.runtime.EntryTypePresentationProvider
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType

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
