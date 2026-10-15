package mihon.entry.interactions.lifecycle.metadata

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.runtime.FeatureRuntimeComposition
import mihon.feature.runtime.injekt.InjektExport
import mihon.feature.runtime.injekt.injektExport

@ContributesTo(AppScope::class)
@BindingContainer
object EntryMetadataLifecycleBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun feature(composition: FeatureRuntimeComposition): EntryMetadataLifecycleFeature =
        EntryMetadataLifecycleCoordinator(composition.executions)

    @Provides
    @IntoSet
    fun graphContributor(): FeatureGraphContributor = EntryMetadataLifecycleFeatureContributor

    @Provides
    @IntoSet
    fun exportFeature(feature: () -> EntryMetadataLifecycleFeature): InjektExport<*> = injektExport(feature)
}
