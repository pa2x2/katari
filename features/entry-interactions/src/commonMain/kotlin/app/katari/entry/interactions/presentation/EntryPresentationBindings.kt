package app.katari.entry.interactions.presentation

import app.katari.entry.interactions.runtime.EntryInteractions
import app.katari.entry.interactions.runtime.EntryTypePresentationProvider
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType

@ContributesTo(AppScope::class)
@BindingContainer
object EntryPresentationBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun feature(
        typePresentationProviders: Map<EntryType, EntryTypePresentationProvider>,
        interactions: EntryInteractions,
    ): EntryTypePresentationFeature = DefaultEntryTypePresentationFeature(
        applicableTypes = typePresentationProviders.keys,
        interaction = interactions.typePresentation,
    )
}
