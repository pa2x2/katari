package mihon.entry.interactions.lifecycle.profile

import mihon.entry.interactions.lifecycle.profile.consequence.ENTRY_PROFILE_MOVE_CUSTOM_COVER_PARTICIPANT
import mihon.entry.interactions.lifecycle.profile.consequence.ENTRY_PROFILE_MOVE_SOURCE_VISIBILITY_PARTICIPANT
import mihon.entry.interactions.lifecycle.profile.consequence.EntryProfileMoveCustomCoverContributor
import mihon.entry.interactions.lifecycle.profile.consequence.EntryProfileMoveHostContributor
import mihon.entry.interactions.runtime.production.EntryFeatureRuntimeArtifacts
import mihon.entry.interactions.runtime.production.EntryFeatureRuntimeModule
import mihon.entry.interactions.runtime.production.entryFeatureRuntimeBoundary
import mihon.feature.graph.execution.FeatureExecutionHandler
import mihon.feature.graph.execution.FeatureExecutionParticipantBinding
import mihon.feature.runtime.FeatureRuntimeComposition
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
            executions = get<FeatureRuntimeComposition>().executions,
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
