package mihon.entry.interactions.lifecycle.profile

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.entry.interactions.lifecycle.profile.consequence.ENTRY_PROFILE_MOVE_CUSTOM_COVER_PARTICIPANT
import mihon.entry.interactions.lifecycle.profile.consequence.ENTRY_PROFILE_MOVE_SOURCE_VISIBILITY_PARTICIPANT
import mihon.entry.interactions.lifecycle.profile.consequence.EntryProfileMoveCustomCoverContributor
import mihon.entry.interactions.lifecycle.profile.consequence.EntryProfileMoveHostContributor
import mihon.entry.interactions.lifecycle.profile.host.EntryProfileMoveCustomCoverHost
import mihon.entry.interactions.lifecycle.profile.host.EntryProfileMoveHost
import mihon.entry.interactions.lifecycle.profile.host.EntryProfileMoveSourceVisibilityHost
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.execution.FeatureExecutionHandler
import mihon.feature.graph.execution.FeatureExecutionParticipantBinding
import mihon.feature.runtime.FeatureRuntimeComposition

@ContributesTo(AppScope::class)
@BindingContainer
object EntryProfileMoveBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun feature(
        profileMoveHost: EntryProfileMoveHost,
        composition: FeatureRuntimeComposition,
    ): EntryProfileMoveFeature = EntryProfileMoveCoordinator(
        host = profileMoveHost,
        executions = composition.executions,
    )

    @Provides
    @IntoSet
    fun graphContributor(): FeatureGraphContributor = EntryProfileMoveFeatureContributor

    @Provides
    @IntoSet
    fun hostGraphContributor(): FeatureGraphContributor = EntryProfileMoveHostContributor

    @Provides
    @IntoSet
    fun customCoverGraphContributor(): FeatureGraphContributor = EntryProfileMoveCustomCoverContributor

    @Provides
    @IntoSet
    fun sourceVisibilityParticipant(
        sourceVisibilityHost: EntryProfileMoveSourceVisibilityHost,
    ): FeatureExecutionParticipantBinding<*> = FeatureExecutionParticipantBinding(
        definition = ENTRY_PROFILE_MOVE_SOURCE_VISIBILITY_PARTICIPANT,
        handler = FeatureExecutionHandler { event ->
            sourceVisibilityHost.makeSourcesVisible(
                event.plan.destinationProfileId,
                event.plan.movedEntries.mapTo(mutableSetOf()) { it.source },
            )
        },
    )

    @Provides
    @IntoSet
    fun customCoverParticipant(
        customCoverHost: EntryProfileMoveCustomCoverHost,
    ): FeatureExecutionParticipantBinding<*> = FeatureExecutionParticipantBinding(
        definition = ENTRY_PROFILE_MOVE_CUSTOM_COVER_PARTICIPANT,
        handler = FeatureExecutionHandler { event ->
            customCoverHost.removeCustomCovers(event.plan.removedEntries)
        },
    )
}
