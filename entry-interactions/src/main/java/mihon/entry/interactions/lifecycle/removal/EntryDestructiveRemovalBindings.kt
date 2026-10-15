package mihon.entry.interactions.lifecycle.removal

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.entry.interactions.lifecycle.removal.consequence.ENTRY_CUSTOM_COVER_DESTRUCTIVE_REMOVAL_PARTICIPANT
import mihon.entry.interactions.lifecycle.removal.consequence.EntryDestructiveRemovalCustomCoverContributor
import mihon.entry.interactions.lifecycle.removal.host.EntryDestructiveRemovalCustomCoverHost
import mihon.entry.interactions.lifecycle.removal.host.EntryDestructiveRemovalHost
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.execution.FeatureExecutionHandler
import mihon.feature.graph.execution.FeatureExecutionParticipantBinding
import mihon.feature.runtime.FeatureRuntimeComposition

@ContributesTo(AppScope::class)
@BindingContainer
object EntryDestructiveRemovalBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun feature(
        destructiveRemovalHost: EntryDestructiveRemovalHost,
        composition: FeatureRuntimeComposition,
    ): EntryDestructiveRemovalFeature = EntryDestructiveRemovalCoordinator(
        host = destructiveRemovalHost,
        executions = composition.executions,
    )

    @Provides
    @IntoSet
    fun graphContributor(): FeatureGraphContributor = EntryDestructiveRemovalFeatureContributor

    @Provides
    @IntoSet
    fun customCoverGraphContributor(): FeatureGraphContributor = EntryDestructiveRemovalCustomCoverContributor

    @Provides
    @IntoSet
    fun customCoverParticipant(
        customCoverHost: EntryDestructiveRemovalCustomCoverHost,
    ): FeatureExecutionParticipantBinding<*> = FeatureExecutionParticipantBinding(
        definition = ENTRY_CUSTOM_COVER_DESTRUCTIVE_REMOVAL_PARTICIPANT,
        handler = FeatureExecutionHandler { event ->
            event.entries.forEach { entry ->
                customCoverHost.removeCustomCover(entry)
            }
        },
    )
}
