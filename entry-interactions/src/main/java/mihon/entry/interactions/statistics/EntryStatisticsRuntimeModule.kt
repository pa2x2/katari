package mihon.entry.interactions.statistics

import mihon.entry.interactions.runtime.EntryInteractions
import mihon.entry.interactions.runtime.production.EntryFeatureRuntimeArtifacts
import mihon.entry.interactions.runtime.production.EntryFeatureRuntimeModule
import mihon.entry.interactions.runtime.production.entryFeatureRuntimeBoundary
import mihon.feature.runtime.FeatureRuntimeComposition
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

internal val EntryStatisticsFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.statistics",
    contributor = EntryStatisticsFeatureContributor,
) {
    addSingletonFactory<EntryStatisticsFeature> {
        DefaultEntryStatisticsFeature(
            evaluation = get<FeatureRuntimeComposition>().evaluation,
            interaction = get<EntryInteractions>().statistics,
        )
    }
    EntryFeatureRuntimeArtifacts(
        runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntryStatisticsFeature>() }),
    )
}
