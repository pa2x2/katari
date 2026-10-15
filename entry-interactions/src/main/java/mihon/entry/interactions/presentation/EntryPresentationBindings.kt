package mihon.entry.interactions.presentation

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.entry.interactions.runtime.EntryInteractions
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.runtime.FeatureRuntimeComposition

@ContributesTo(AppScope::class)
@BindingContainer
object EntryPresentationBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun feature(
        composition: FeatureRuntimeComposition,
        interactions: EntryInteractions,
    ): EntryTypePresentationFeature = DefaultEntryTypePresentationFeature(
        evaluation = composition.evaluation,
        interaction = interactions.typePresentation,
    )

    @Provides
    @IntoSet
    fun graphContributor(): FeatureGraphContributor = EntryTypePresentationFeatureContributor
}
