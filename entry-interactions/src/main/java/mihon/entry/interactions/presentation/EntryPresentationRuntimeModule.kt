package mihon.entry.interactions

import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

internal val EntryTypePresentationFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.type-presentation",
    contributor = EntryTypePresentationFeatureContributor,
) {
    addSingletonFactory<EntryTypePresentationFeature> {
        val composition = get<EntryInteractionComposition>()
        DefaultEntryTypePresentationFeature(
            evaluation = composition.featureGraphEvaluation,
            interaction = composition.interactions.typePresentation,
        )
    }
    EntryFeatureRuntimeArtifacts(
        runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntryTypePresentationFeature>() }),
    )
}
