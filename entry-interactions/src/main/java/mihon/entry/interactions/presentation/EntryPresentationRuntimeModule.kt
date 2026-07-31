package mihon.entry.interactions.presentation

import mihon.entry.interactions.runtime.EntryInteractions
import mihon.entry.interactions.runtime.production.EntryFeatureRuntimeArtifacts
import mihon.entry.interactions.runtime.production.EntryFeatureRuntimeModule
import mihon.entry.interactions.runtime.production.entryFeatureRuntimeBoundary
import mihon.feature.runtime.FeatureRuntimeComposition
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

internal val EntryTypePresentationFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.type-presentation",
    contributor = EntryTypePresentationFeatureContributor,
) {
    addSingletonFactory<EntryTypePresentationFeature> {
        val composition = get<FeatureRuntimeComposition>()
        DefaultEntryTypePresentationFeature(
            evaluation = composition.evaluation,
            interaction = get<EntryInteractions>().typePresentation,
        )
    }
    EntryFeatureRuntimeArtifacts(
        runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntryTypePresentationFeature>() }),
    )
}
