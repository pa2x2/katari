package mihon.entry.interactions.presentation

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.runtime.EntryInteractions
import mihon.entry.interactions.runtime.EntryTypePresentationProvider

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
