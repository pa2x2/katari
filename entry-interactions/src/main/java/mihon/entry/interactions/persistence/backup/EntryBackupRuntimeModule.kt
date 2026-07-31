package mihon.entry.interactions.persistence.backup

import mihon.entry.interactions.runtime.production.EntryFeatureRuntimeArtifacts
import mihon.entry.interactions.runtime.production.EntryFeatureRuntimeModule
import mihon.entry.interactions.runtime.production.entryFeatureRuntimeBoundary
import mihon.feature.runtime.FeatureRuntimeComposition
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

internal val EntryBackupFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.backup",
    contributor = EntryBackupFeatureContributor,
) {
    addSingletonFactory<EntryBackupFeature> {
        EntryBackupCoordinator(get<FeatureRuntimeComposition>().executions)
    }
    EntryFeatureRuntimeArtifacts(
        runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntryBackupFeature>() }),
    )
}
