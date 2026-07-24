package mihon.entry.interactions

import mihon.feature.graph.FeatureGraphContributor

/** Graph-only view for cross-module content-type validation. Production runtime must install the modules themselves. */
fun productionEntryFeatureGraphForValidation(): List<FeatureGraphContributor> {
    return productionEntryFeatureRuntimeModules().flatMap(EntryFeatureRuntimeModule::graphContributors)
}
