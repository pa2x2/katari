package mihon.entry.interactions

import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

internal val EntryBackupFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.backup",
    contributor = EntryBackupFeatureContributor,
) {
    addSingletonFactory<EntryBackupFeature> {
        EntryBackupCoordinator(get<EntryInteractionComposition>().featureExecutions)
    }
    EntryFeatureRuntimeArtifacts(
        runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntryBackupFeature>() }),
    )
}
