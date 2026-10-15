package mihon.feature.runtime

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.feature.runtime.injekt.InjektExport
import mihon.feature.runtime.injekt.injektExport

@ContributesTo(AppScope::class)
@BindingContainer
object FeatureRuntimeCompositionBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun composition(contributions: FeatureRuntimeContributions): FeatureRuntimeComposition =
        createFeatureRuntimeComposition(listOf(contributions.featureRuntimeInputs))

    @Provides
    @IntoSet
    fun exportComposition(composition: () -> FeatureRuntimeComposition): InjektExport<*> = injektExport(composition)
}
