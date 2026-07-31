package mihon.entry.interactions.lifecycle.removal

import mihon.entry.interactions.lifecycle.removal.consequence.ENTRY_CUSTOM_COVER_DESTRUCTIVE_REMOVAL_PARTICIPANT
import mihon.entry.interactions.lifecycle.removal.consequence.EntryDestructiveRemovalCustomCoverContributor
import mihon.entry.interactions.runtime.production.EntryFeatureRuntimeArtifacts
import mihon.entry.interactions.runtime.production.EntryFeatureRuntimeModule
import mihon.entry.interactions.runtime.production.entryFeatureRuntimeBoundary
import mihon.feature.graph.execution.FeatureExecutionHandler
import mihon.feature.graph.execution.FeatureExecutionParticipantBinding
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
                        context.dependencies.destructiveRemovalCustomCoverHost.removeCustomCover(
                            entry,
                        )
                    }
                },
            ),
        ),
        runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntryDestructiveRemovalFeature>() }),
    )
}
