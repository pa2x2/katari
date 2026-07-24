package mihon.entry.interactions

import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

internal val EntryOpenFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.open",
    contributor = EntryOpenFeatureContributor,
) {
    addSingletonFactory<EntryOpenFeature> {
        val composition = get<EntryInteractionComposition>()
        DefaultEntryOpenFeature(
            evaluation = composition.featureGraphEvaluation,
            interaction = composition.interactions.open,
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
        val composition = get<EntryInteractionComposition>()
        DefaultEntryContinueFeature(
            evaluation = composition.featureGraphEvaluation,
            interaction = composition.interactions.continueEntry,
        )
    }
    EntryFeatureRuntimeArtifacts(
        runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntryContinueFeature>() }),
    )
}
