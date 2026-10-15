package mihon.entry.interactions.validation

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import mihon.feature.runtime.FeatureRuntimeContributions

/** The Entry Features this module contributes to the production graph. */
@DependencyGraph(AppScope::class)
interface EntryInteractionValidationGraph {
    val featureRuntimeContributions: FeatureRuntimeContributions
}
