package mihon.entry.interactions.statistics

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.entry.interactions.runtime.EntryInteractions
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.runtime.FeatureRuntimeComposition
import mihon.feature.runtime.injekt.InjektExport
import mihon.feature.runtime.injekt.injektExport

@ContributesTo(AppScope::class)
@BindingContainer
object EntryStatisticsBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun feature(composition: FeatureRuntimeComposition, interactions: EntryInteractions): EntryStatisticsFeature =
        DefaultEntryStatisticsFeature(
            evaluation = composition.evaluation,
            interaction = interactions.statistics,
        )

    @Provides
    @IntoSet
    fun graphContributor(): FeatureGraphContributor = EntryStatisticsFeatureContributor

    @Provides
    @IntoSet
    fun exportFeature(feature: () -> EntryStatisticsFeature): InjektExport<*> = injektExport(feature)
}
