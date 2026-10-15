package mihon.entry.interactions.validation

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import mihon.feature.runtime.FeatureRuntimeGraph

/** The Entry Features this module contributes, with the host services the validation environment controls. */
@DependencyGraph(AppScope::class)
interface EntryInteractionValidationGraph : FeatureRuntimeGraph
