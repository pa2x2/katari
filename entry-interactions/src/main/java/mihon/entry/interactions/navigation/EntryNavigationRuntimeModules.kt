package mihon.entry.interactions.navigation

import mihon.entry.interactions.runtime.EntryInteractions
import mihon.entry.interactions.runtime.production.EntryFeatureRuntimeArtifacts
import mihon.entry.interactions.runtime.production.EntryFeatureRuntimeModule
import mihon.entry.interactions.runtime.production.entryFeatureRuntimeBoundary
import mihon.feature.runtime.FeatureRuntimeComposition
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

internal val EntryOpenFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.open",
    contributor = EntryOpenFeatureContributor,
) {
    addSingletonFactory<EntryOpenFeature> {
        val composition = get<FeatureRuntimeComposition>()
        DefaultEntryOpenFeature(
            evaluation = composition.evaluation,
            interaction = get<EntryInteractions>().open,
        )
    }
    EntryFeatureRuntimeArtifacts(
        runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntryOpenFeature>() }),
    )
}

internal val EntryContinueFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.continue",
    contributor = EntryContinueFeatureContributor,
) {
    addSingletonFactory<EntryContinueFeature> {
        val composition = get<FeatureRuntimeComposition>()
        DefaultEntryContinueFeature(
            evaluation = composition.evaluation,
            interaction = get<EntryInteractions>().continueEntry,
            batchPreparation = DefaultEntryContinueBatchPreparation(
                childOwnership = get(),
                chapterRepository = get(),
                progressRepository = get(),
            ),
        )
    }
    EntryFeatureRuntimeArtifacts(
        runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntryContinueFeature>() }),
    )
}
