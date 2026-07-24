package mihon.entry.interactions

import mihon.feature.graph.FeatureExecutionHandler
import mihon.feature.graph.FeatureExecutionParticipantBinding
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

internal val EntryProfileMoveFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.profile-move",
    contributor = EntryProfileMoveFeatureContributor,
    additionalContributors = listOf(
        EntryProfileMoveHostContributor,
        EntryProfileMoveCustomCoverContributor,
    ),
) { context ->
    addSingletonFactory<EntryProfileMoveFeature> {
        EntryProfileMoveCoordinator(
            host = context.dependencies.profileMoveHost,
            executions = get<EntryInteractionComposition>().featureExecutions,
        )
    }
    EntryFeatureRuntimeArtifacts(
        executionBindings = listOf(
            FeatureExecutionParticipantBinding(
                definition = ENTRY_PROFILE_MOVE_SOURCE_VISIBILITY_PARTICIPANT,
                handler = FeatureExecutionHandler { event ->
                    context.dependencies.profileMoveSourceVisibilityHost.makeSourcesVisible(
                        event.plan.destinationProfileId,
                        event.plan.movedEntries.mapTo(mutableSetOf()) { it.source },
                    )
                },
            ),
            FeatureExecutionParticipantBinding(
                definition = ENTRY_PROFILE_MOVE_CUSTOM_COVER_PARTICIPANT,
                handler = FeatureExecutionHandler { event ->
                    context.dependencies.profileMoveCustomCoverHost.removeCustomCovers(event.plan.removedEntries)
                },
            ),
        ),
        runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntryProfileMoveFeature>() }),
    )
}
