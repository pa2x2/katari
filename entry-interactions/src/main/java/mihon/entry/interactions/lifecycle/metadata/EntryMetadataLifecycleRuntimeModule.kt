package mihon.entry.interactions

import mihon.feature.runtime.FeatureRuntimeComposition
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

internal val EntryMetadataLifecycleFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.metadata-lifecycle",
    contributor = EntryMetadataLifecycleFeatureContributor,
) {
    addSingletonFactory<EntryMetadataLifecycleFeature> {
        EntryMetadataLifecycleCoordinator(get<FeatureRuntimeComposition>().executions)
    }
    EntryFeatureRuntimeArtifacts(
        runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntryMetadataLifecycleFeature>() }),
    )
}
