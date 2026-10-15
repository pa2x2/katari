package mihon.entry.interactions.child

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.entry.interactions.child.backup.EntryChildGroupFilterBackupParticipant
import mihon.entry.interactions.child.lifecycle.ENTRY_CHILD_GROUP_FILTER_PROFILE_MOVE_PARTICIPANT
import mihon.entry.interactions.child.lifecycle.EntryChildGroupFilterProfileMoveContributor
import mihon.entry.interactions.lifecycle.profile.host.EntryProfileMoveChildGroupFilterStateHost
import mihon.entry.interactions.lifecycle.profile.stateRequest
import mihon.entry.interactions.persistence.backup.EntryBackupStateParticipant
import mihon.entry.interactions.runtime.EntryChildGroupFilterDataSource
import mihon.entry.interactions.runtime.EntryInteractions
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.execution.FeatureExecutionHandler
import mihon.feature.graph.execution.FeatureExecutionParticipantBinding
import mihon.feature.runtime.FeatureRuntimeComposition
import tachiyomi.domain.entry.interactor.GetEntry
import tachiyomi.domain.entry.interactor.NetworkToLocalEntry
import tachiyomi.domain.source.service.EntrySourceDescriptionResolutionPort
import tachiyomi.domain.source.service.SourceManager

@ContributesTo(AppScope::class)
@BindingContainer
object EntryChildBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun childListFeature(
        composition: FeatureRuntimeComposition,
        interactions: EntryInteractions,
    ): EntryChildListFeature = DefaultEntryChildListFeature(
        evaluation = composition.evaluation,
        childList = interactions.childList,
        childProgress = interactions.childProgress,
        missingChildGap = interactions.missingChildGap,
    )

    @Provides
    @SingleIn(AppScope::class)
    fun childGroupFilterFeature(
        composition: FeatureRuntimeComposition,
        interactions: EntryInteractions,
        dataSource: EntryChildGroupFilterDataSource,
    ): EntryChildGroupFilterFeature = DefaultEntryChildGroupFilterFeature(
        evaluation = composition.evaluation,
        interaction = interactions.childGroupFilter,
        dataSource = dataSource,
    )

    @Provides
    @SingleIn(AppScope::class)
    fun relatedEntriesFeature(
        composition: FeatureRuntimeComposition,
        sourceManager: SourceManager,
        networkToLocalEntry: NetworkToLocalEntry,
        getEntry: GetEntry,
        sourceDescription: EntrySourceDescriptionResolutionPort,
    ): EntryRelatedEntriesFeature = DefaultEntryRelatedEntriesFeature(
        evaluation = composition.evaluation,
        sourceManager = sourceManager,
        networkToLocalEntry = networkToLocalEntry,
        getEntry = getEntry,
        sourceDescription = sourceDescription,
    )

    @Provides
    @IntoSet
    fun childListGraphContributor(): FeatureGraphContributor = EntryChildListFeatureContributor

    @Provides
    @IntoSet
    fun childGroupFilterGraphContributor(): FeatureGraphContributor = EntryChildGroupFilterFeatureContributor

    @Provides
    @IntoSet
    fun childGroupFilterProfileMoveGraphContributor(): FeatureGraphContributor =
        EntryChildGroupFilterProfileMoveContributor

    @Provides
    @IntoSet
    fun relatedEntriesGraphContributor(): FeatureGraphContributor = EntryRelatedEntriesFeatureContributor

    @Provides
    @IntoSet
    fun childGroupFilterProfileMoveParticipant(
        profileMoveChildGroupFilterStateHost: EntryProfileMoveChildGroupFilterStateHost,
    ): FeatureExecutionParticipantBinding<*> = FeatureExecutionParticipantBinding(
        definition = ENTRY_CHILD_GROUP_FILTER_PROFILE_MOVE_PARTICIPANT,
        handler = FeatureExecutionHandler { event ->
            profileMoveChildGroupFilterStateHost.move(event.plan.stateRequest())
        },
    )

    @Provides
    @IntoSet
    internal fun childGroupFilterBackupState(
        feature: () -> EntryChildGroupFilterFeature,
    ): EntryBackupStateParticipant = EntryChildGroupFilterBackupParticipant(feature)
}
