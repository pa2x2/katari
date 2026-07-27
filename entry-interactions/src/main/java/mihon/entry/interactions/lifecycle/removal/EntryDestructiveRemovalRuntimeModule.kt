package mihon.entry.interactions

import mihon.feature.graph.FeatureExecutionHandler
import mihon.feature.graph.FeatureExecutionParticipantBinding
import mihon.feature.runtime.FeatureRuntimeComposition
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

internal val EntryDestructiveRemovalFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.destructive-removal",
    contributor = EntryDestructiveRemovalFeatureContributor,
    additionalContributors = listOf(EntryDestructiveRemovalCustomCoverContributor),
) { context ->
    addSingletonFactory<EntryDestructiveRemovalFeature> {
        EntryDestructiveRemovalCoordinator(
            host = context.dependencies.destructiveRemovalHost,
            executions = get<FeatureRuntimeComposition>().executions,
        )
    }
    EntryFeatureRuntimeArtifacts(
        executionBindings = listOf(
            FeatureExecutionParticipantBinding(
                definition = ENTRY_CUSTOM_COVER_DESTRUCTIVE_REMOVAL_PARTICIPANT,
                handler = FeatureExecutionHandler { event ->
                    event.entries.forEach { entry ->
                        context.dependencies.destructiveRemovalCustomCoverHost.removeCustomCover(entry)
                    }
                },
            ),
        ),
        runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntryDestructiveRemovalFeature>() }),
    )
}
