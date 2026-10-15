package mihon.entry.interactions.child

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.entry.interactions.child.backup.ENTRY_CHILD_GROUP_FILTER_BACKUP_RESTORE_PARTICIPANT
import mihon.entry.interactions.child.backup.ENTRY_CHILD_GROUP_FILTER_BACKUP_SCHEMA_VERSION
import mihon.entry.interactions.child.backup.ENTRY_CHILD_GROUP_FILTER_BACKUP_SNAPSHOT_PARTICIPANT
import mihon.entry.interactions.child.backup.ENTRY_CHILD_GROUP_FILTER_BACKUP_STATE_ID
import mihon.entry.interactions.child.backup.EntryChildGroupFilterBackupContributor
import mihon.entry.interactions.child.backup.EntryChildGroupFilterBackupState
import mihon.entry.interactions.child.lifecycle.ENTRY_CHILD_GROUP_FILTER_PROFILE_MOVE_PARTICIPANT
import mihon.entry.interactions.child.lifecycle.EntryChildGroupFilterProfileMoveContributor
import mihon.entry.interactions.lifecycle.profile.host.EntryProfileMoveChildGroupFilterStateHost
import mihon.entry.interactions.lifecycle.profile.stateRequest
import mihon.entry.interactions.persistence.backup.decodeEntryBackupState
import mihon.entry.interactions.persistence.backup.entryBackupStateEnvelope
import mihon.entry.interactions.runtime.EntryChildGroupFilterDataSource
import mihon.entry.interactions.runtime.EntryInteractions
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.execution.FeatureExecutionHandler
import mihon.feature.graph.execution.FeatureExecutionParticipantBinding
import mihon.feature.runtime.FeatureRuntimeComposition
import mihon.feature.runtime.injekt.InjektExport
import mihon.feature.runtime.injekt.injektExport
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
    fun childGroupFilterBackupGraphContributor(): FeatureGraphContributor = EntryChildGroupFilterBackupContributor

    @Provides
    @IntoSet
    fun relatedEntriesGraphContributor(): FeatureGraphContributor = EntryRelatedEntriesFeatureContributor

    @Provides
    @IntoSet
    fun childGroupFilterBackupSnapshotParticipant(
        feature: () -> EntryChildGroupFilterFeature,
    ): FeatureExecutionParticipantBinding<*> = FeatureExecutionParticipantBinding(
        definition = ENTRY_CHILD_GROUP_FILTER_BACKUP_SNAPSHOT_PARTICIPANT,
        handler = FeatureExecutionHandler { event ->
            val result = feature().snapshot(event.profileId, event.entry)
            if (result is EntryChildGroupFilterSnapshotResult.Available && result.excludedGroups.isNotEmpty()) {
                event.contributions.add(
                    entryBackupStateEnvelope(
                        ENTRY_CHILD_GROUP_FILTER_BACKUP_STATE_ID,
                        ENTRY_CHILD_GROUP_FILTER_BACKUP_SCHEMA_VERSION,
                        EntryChildGroupFilterBackupState.serializer(),
                        EntryChildGroupFilterBackupState(result.excludedGroups),
                    ),
                )
            }
        },
    )

    @Provides
    @IntoSet
    fun childGroupFilterBackupRestoreParticipant(
        feature: () -> EntryChildGroupFilterFeature,
    ): FeatureExecutionParticipantBinding<*> = FeatureExecutionParticipantBinding(
        definition = ENTRY_CHILD_GROUP_FILTER_BACKUP_RESTORE_PARTICIPANT,
        handler = FeatureExecutionHandler { event ->
            val state = event.states.decodeEntryBackupState(
                ENTRY_CHILD_GROUP_FILTER_BACKUP_STATE_ID,
                ENTRY_CHILD_GROUP_FILTER_BACKUP_SCHEMA_VERSION,
                EntryChildGroupFilterBackupState.serializer(),
            ) ?: return@FeatureExecutionHandler
            feature().restore(event.entry, state.excludedGroups)
        },
    )

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
    fun exportChildListFeature(feature: () -> EntryChildListFeature): InjektExport<*> = injektExport(feature)

    @Provides
    @IntoSet
    fun exportChildGroupFilterFeature(feature: () -> EntryChildGroupFilterFeature): InjektExport<*> =
        injektExport(feature)

    @Provides
    @IntoSet
    fun exportRelatedEntriesFeature(feature: () -> EntryRelatedEntriesFeature): InjektExport<*> =
        injektExport(feature)
}
