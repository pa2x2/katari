package mihon.entry.interactions.validation

import mihon.entry.interactions.runtime.EntryInteractionComposition
import mihon.feature.runtime.FeatureRuntimeComposition
import mihon.feature.runtime.FeatureRuntimeGraph

/** What the validation environment reads from a production validation graph. */
interface ProductionValidationGraph : FeatureRuntimeGraph {
    val featureRuntimeComposition: FeatureRuntimeComposition
    val entryInteractionComposition: EntryInteractionComposition
}
