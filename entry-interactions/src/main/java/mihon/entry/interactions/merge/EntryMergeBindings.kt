package mihon.entry.interactions.merge

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mihon.core.metro.AppCoroutineScope
import mihon.entry.interactions.download.EntryDownloadMaintenanceFeature
import mihon.entry.interactions.library.membership.host.EntryLibraryCustomCoverHost
import mihon.entry.interactions.merge.backup.ENTRY_MERGE_BACKUP_FINALIZE_PARTICIPANT
import mihon.entry.interactions.merge.backup.ENTRY_MERGE_BACKUP_RESTORE_PARTICIPANT
import mihon.entry.interactions.merge.backup.ENTRY_MERGE_BACKUP_SNAPSHOT_PARTICIPANT
import mihon.entry.interactions.merge.backup.EntryMergeBackupContributor
import mihon.entry.interactions.merge.backup.EntryMergeBackupRestoreParticipation
import mihon.entry.interactions.merge.consequence.EntryMergeConsequenceDelivery
import mihon.entry.interactions.merge.consequence.EntryMergeDurableConsequences
import mihon.entry.interactions.merge.consequence.cover.EntryMergeCustomCoverContributor
import mihon.entry.interactions.merge.consequence.cover.entryMergeCustomCoverBinding
import mihon.entry.interactions.merge.consequence.legacy.EntryMergeLegacyConsequenceDelivery
import mihon.entry.interactions.merge.host.EntryMergeHost
import mihon.entry.interactions.merge.lifecycle.ENTRY_MERGE_DESTRUCTIVE_REMOVAL_PARTICIPANT
import mihon.entry.interactions.merge.lifecycle.ENTRY_MERGE_PROFILE_MOVE_DESTINATION_PARTICIPANT
import mihon.entry.interactions.merge.lifecycle.ENTRY_MERGE_PROFILE_MOVE_PARTICIPANT_ID
import mihon.entry.interactions.merge.lifecycle.ENTRY_MERGE_PROFILE_MOVE_PREPARATION_PARTICIPANT
import mihon.entry.interactions.merge.lifecycle.ENTRY_MERGE_PROFILE_MOVING_PARTICIPANT
import mihon.entry.interactions.merge.lifecycle.ENTRY_MERGE_PROFILE_STATE_MOVED_PARTICIPANT
import mihon.entry.interactions.merge.lifecycle.EntryMergeDestructiveRemovalContributor
import mihon.entry.interactions.merge.lifecycle.EntryMergeProfileMoveContributor
import mihon.entry.interactions.merge.lifecycle.mergeReference
import mihon.entry.interactions.merge.lifecycle.toMergeIntent
import mihon.entry.interactions.persistence.backup.decodeEntryBackupState
import mihon.entry.interactions.persistence.backup.entryBackupStateEnvelope
import mihon.entry.interactions.tracking.EntryTrackingFeature
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.execution.FeatureDurableExecutionParticipantBinding
import mihon.feature.graph.execution.FeatureExecutionHandler
import mihon.feature.graph.execution.FeatureExecutionParticipantBinding
import mihon.feature.runtime.FeatureRuntimeComposition
import mihon.feature.runtime.FeatureRuntimeWarmup
import mihon.feature.runtime.injekt.InjektExport
import mihon.feature.runtime.injekt.injektExport
import tachiyomi.domain.entry.service.EntryChildOwnershipResolutionPort
import tachiyomi.domain.entry.service.EntryLibraryGroupingResolutionPort

@ContributesTo(AppScope::class)
@BindingContainer
object EntryMergeBindings {
    @Provides
    @SingleIn(AppScope::class)
    internal fun durableConsequences(composition: FeatureRuntimeComposition): EntryMergeDurableConsequences =
        EntryMergeDurableConsequences(composition.executions)

    @Provides
    @SingleIn(AppScope::class)
    internal fun legacyConsequenceDelivery(
        mergeHost: EntryMergeHost,
        tracking: () -> EntryTrackingFeature,
        libraryCustomCoverHost: EntryLibraryCustomCoverHost,
        downloadMaintenance: () -> EntryDownloadMaintenanceFeature,
    ): EntryMergeLegacyConsequenceDelivery = EntryMergeLegacyConsequenceDelivery(
        host = mergeHost,
        tracking = tracking,
        coverCleanup = libraryCustomCoverHost::cleanupAfterLibraryRemoval,
        downloadMaintenance = downloadMaintenance,
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun consequenceDelivery(
        mergeHost: EntryMergeHost,
        consequences: EntryMergeDurableConsequences,
        legacy: EntryMergeLegacyConsequenceDelivery,
    ): EntryMergeConsequenceDelivery = EntryMergeConsequenceDelivery(
        host = mergeHost,
        consequences = consequences,
        legacy = legacy,
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun feature(
        composition: FeatureRuntimeComposition,
        mergeHost: EntryMergeHost,
        durableConsequences: EntryMergeDurableConsequences,
        consequences: EntryMergeConsequenceDelivery,
    ): EntryMergeFeature = EntryMergeWorkflowCoordinator(
        evaluation = composition.evaluation,
        host = mergeHost,
        durableConsequences = durableConsequences,
        consequences = consequences,
    )

    @Provides
    @SingleIn(AppScope::class)
    fun candidateFeature(mergeHost: EntryMergeHost): EntryMergeCandidateFeature =
        EntryMergeCandidateCoordinator(mergeHost)

    @Provides
    @SingleIn(AppScope::class)
    fun navigationFeature(mergeHost: EntryMergeHost): EntryMergeNavigationFeature =
        EntryMergeNavigationCoordinator(mergeHost)

    @Provides
    @SingleIn(AppScope::class)
    internal fun libraryGroupingCoordinator(mergeHost: EntryMergeHost): EntryMergeLibraryGroupingCoordinator =
        EntryMergeLibraryGroupingCoordinator(mergeHost)

    @Provides
    internal fun libraryGroupingFeature(
        coordinator: EntryMergeLibraryGroupingCoordinator,
    ): EntryMergeLibraryGroupingFeature = coordinator

    @Provides
    internal fun libraryGroupingResolutionPort(
        coordinator: EntryMergeLibraryGroupingCoordinator,
    ): EntryLibraryGroupingResolutionPort = coordinator

    @Provides
    @SingleIn(AppScope::class)
    fun backupFeature(mergeHost: EntryMergeHost): EntryMergeBackupFeature = EntryMergeBackupCoordinator(mergeHost)

    @Provides
    @SingleIn(AppScope::class)
    internal fun backupRestoreParticipation(feature: EntryMergeBackupFeature): EntryMergeBackupRestoreParticipation =
        EntryMergeBackupRestoreParticipation(feature)

    @Provides
    @SingleIn(AppScope::class)
    fun libraryLifecycleFeature(mergeHost: EntryMergeHost): EntryMergeLibraryLifecycleFeature =
        EntryMergeLibraryLifecycleCoordinator(mergeHost)

    @Provides
    @SingleIn(AppScope::class)
    fun metadataRefreshFeature(mergeHost: EntryMergeHost): EntryMergeMetadataRefreshFeature =
        EntryMergeMetadataRefreshCoordinator(mergeHost)

    @Provides
    @SingleIn(AppScope::class)
    fun profileMoveFeature(mergeHost: EntryMergeHost): EntryMergeProfileMoveFeature =
        EntryMergeProfileMoveCoordinator(mergeHost)

    @Provides
    @SingleIn(AppScope::class)
    internal fun consequenceStatusFeature(
        mergeHost: EntryMergeHost,
        delivery: EntryMergeConsequenceDelivery,
    ): EntryMergeConsequenceStatusFeature = EntryMergeConsequenceStatusCoordinator(mergeHost, delivery)

    @Provides
    @SingleIn(AppScope::class)
    fun migrationFeature(mergeHost: EntryMergeHost): EntryMergeMigrationFeature =
        EntryMergeMigrationCoordinator(mergeHost)

    @Provides
    @SingleIn(AppScope::class)
    internal fun childOwnershipProjection(mergeHost: EntryMergeHost): EntryMergeChildOwnershipProjection =
        EntryMergeChildOwnershipCoordinator(mergeHost)

    @Provides
    internal fun childOwnershipResolutionPort(
        projection: EntryMergeChildOwnershipProjection,
    ): EntryChildOwnershipResolutionPort = projection

    @Provides
    @SingleIn(AppScope::class)
    internal fun downloadOwnershipProjection(mergeHost: EntryMergeHost): EntryMergeDownloadOwnershipProjection =
        EntryMergeDownloadOwnershipCoordinator(mergeHost)

    @Provides
    @IntoSet
    fun graphContributor(): FeatureGraphContributor = EntryMergeFeatureContributor

    @Provides
    @IntoSet
    fun libraryMembershipGraphContributor(): FeatureGraphContributor = EntryMergeLibraryMembershipContributor

    @Provides
    @IntoSet
    fun destructiveRemovalGraphContributor(): FeatureGraphContributor = EntryMergeDestructiveRemovalContributor

    @Provides
    @IntoSet
    fun profileMoveGraphContributor(): FeatureGraphContributor = EntryMergeProfileMoveContributor

    @Provides
    @IntoSet
    fun backupGraphContributor(): FeatureGraphContributor = EntryMergeBackupContributor

    @Provides
    @IntoSet
    fun customCoverGraphContributor(): FeatureGraphContributor = EntryMergeCustomCoverContributor

    @Provides
    @IntoSet
    fun customCoverParticipant(
        mergeHost: EntryMergeHost,
        libraryCustomCoverHost: EntryLibraryCustomCoverHost,
    ): FeatureDurableExecutionParticipantBinding<*> = entryMergeCustomCoverBinding(
        mergeHost,
        libraryCustomCoverHost::cleanupAfterLibraryRemoval,
    )

    @Provides
    @IntoSet
    fun backupSnapshotParticipant(
        backupFeature: () -> EntryMergeBackupFeature,
    ): FeatureExecutionParticipantBinding<*> = FeatureExecutionParticipantBinding(
        definition = ENTRY_MERGE_BACKUP_SNAPSHOT_PARTICIPANT,
        handler = FeatureExecutionHandler { event ->
            val state = backupFeature().snapshotForBackup(
                EntryMergeSubject(event.profileId, event.entry.id),
            ) ?: return@FeatureExecutionHandler
            event.contributions.add(
                entryBackupStateEnvelope(
                    ENTRY_MERGE_BACKUP_STATE_ID,
                    ENTRY_MERGE_BACKUP_SCHEMA_VERSION,
                    EntryMergeBackupMember.serializer(),
                    state,
                ),
            )
        },
    )

    @Provides
    @IntoSet
    internal fun backupRestoreParticipant(
        restoreParticipation: () -> EntryMergeBackupRestoreParticipation,
    ): FeatureExecutionParticipantBinding<*> = FeatureExecutionParticipantBinding(
        definition = ENTRY_MERGE_BACKUP_RESTORE_PARTICIPANT,
        handler = FeatureExecutionHandler { event ->
            val state = event.states.decodeEntryBackupState(
                ENTRY_MERGE_BACKUP_STATE_ID,
                ENTRY_MERGE_BACKUP_SCHEMA_VERSION,
                EntryMergeBackupMember.serializer(),
            ) ?: return@FeatureExecutionHandler
            restoreParticipation().enqueue(event, state)
        },
    )

    @Provides
    @IntoSet
    internal fun backupFinalizeParticipant(
        restoreParticipation: () -> EntryMergeBackupRestoreParticipation,
    ): FeatureExecutionParticipantBinding<*> = FeatureExecutionParticipantBinding(
        definition = ENTRY_MERGE_BACKUP_FINALIZE_PARTICIPANT,
        handler = FeatureExecutionHandler { event ->
            restoreParticipation().finalize(event)
        },
    )

    @Provides
    @IntoSet
    fun libraryRemovalParticipant(
        libraryLifecycle: () -> EntryMergeLibraryLifecycleFeature,
    ): FeatureExecutionParticipantBinding<*> = FeatureExecutionParticipantBinding(
        definition = ENTRY_MERGE_LIBRARY_REMOVAL_PARTICIPANT,
        handler = FeatureExecutionHandler { event ->
            val result = libraryLifecycle().entriesRemovedFromLibrary(event.entries)
            check(result.unresolvedGroupCount == 0) {
                "Merge membership changed while removing Library entries"
            }
        },
    )

    @Provides
    @IntoSet
    fun destructiveRemovalParticipant(
        libraryLifecycle: () -> EntryMergeLibraryLifecycleFeature,
    ): FeatureExecutionParticipantBinding<*> = FeatureExecutionParticipantBinding(
        definition = ENTRY_MERGE_DESTRUCTIVE_REMOVAL_PARTICIPANT,
        handler = FeatureExecutionHandler { event ->
            val result = libraryLifecycle().entriesRemovedFromLibrary(event.entries)
            check(result.unresolvedGroupCount == 0) {
                "Merge membership changed during destructive Entry removal"
            }
        },
    )

    @Provides
    @IntoSet
    fun profileMovePreparationParticipant(
        profileMove: () -> EntryMergeProfileMoveFeature,
    ): FeatureExecutionParticipantBinding<*> = FeatureExecutionParticipantBinding(
        definition = ENTRY_MERGE_PROFILE_MOVE_PREPARATION_PARTICIPANT,
        handler = FeatureExecutionHandler { event ->
            when (
                val result = profileMove().prepare(
                    event.request.sourceProfileId,
                    event.selectedEntries.map { it.id },
                )
            ) {
                is EntryMergeProfileMovePreparationResult.Ready -> {
                    result.units.forEach { unit -> event.contributions.addAtomicUnit(unit.entries) }
                    event.contributions.setParticipantReference(
                        ENTRY_MERGE_PROFILE_MOVE_PARTICIPANT_ID,
                        event.selectedEntries.first().type,
                        result.reference,
                    )
                }

                EntryMergeProfileMovePreparationResult.Empty -> Unit
            }
        },
    )

    @Provides
    @IntoSet
    fun profileMoveDestinationParticipant(
        profileMove: () -> EntryMergeProfileMoveFeature,
    ): FeatureExecutionParticipantBinding<*> = FeatureExecutionParticipantBinding(
        definition = ENTRY_MERGE_PROFILE_MOVE_DESTINATION_PARTICIPANT,
        handler = FeatureExecutionHandler { event ->
            val reference = event.contributions.participantReference(
                ENTRY_MERGE_PROFILE_MOVE_PARTICIPANT_ID,
                event.type,
            ) as? EntryMergeProfileMoveReference ?: return@FeatureExecutionHandler
            when (
                val result = profileMove().inspectDestination(
                    reference,
                    event.request.destinationProfileId,
                    event.conflicts.map { it.destinationEntry.id },
                )
            ) {
                is EntryMergeProfileMoveDestinationResult.Ready -> {
                    event.contributions.setParticipantReference(
                        ENTRY_MERGE_PROFILE_MOVE_PARTICIPANT_ID,
                        event.type,
                        result.reference,
                    )
                    event.contributions.markDestinationAffected(result.mergeAffectedEntryIds)
                }

                EntryMergeProfileMoveDestinationResult.InvalidReference -> {
                    error("Merge Profile-move destination changed during preparation")
                }
            }
        },
    )

    @Provides
    @IntoSet
    fun profileMovingParticipant(
        profileMove: () -> EntryMergeProfileMoveFeature,
    ): FeatureExecutionParticipantBinding<*> = FeatureExecutionParticipantBinding(
        definition = ENTRY_MERGE_PROFILE_MOVING_PARTICIPANT,
        handler = FeatureExecutionHandler { event ->
            val reference = event.reference.mergeReference(event.type) ?: return@FeatureExecutionHandler
            profileMove().begin(event.plan.toMergeIntent(reference)).requireApplied()
        },
    )

    @Provides
    @IntoSet
    fun profileStateMovedParticipant(
        profileMove: () -> EntryMergeProfileMoveFeature,
    ): FeatureExecutionParticipantBinding<*> = FeatureExecutionParticipantBinding(
        definition = ENTRY_MERGE_PROFILE_STATE_MOVED_PARTICIPANT,
        handler = FeatureExecutionHandler { event ->
            val reference = event.reference.mergeReference(event.type) ?: return@FeatureExecutionHandler
            profileMove().complete(event.plan.toMergeIntent(reference)).requireApplied()
        },
    )

    @Provides
    @IntoSet
    internal fun consequenceRetryWarmup(
        @AppCoroutineScope scope: CoroutineScope,
        delivery: () -> EntryMergeConsequenceDelivery,
    ): FeatureRuntimeWarmup = FeatureRuntimeWarmup {
        scope.launch {
            delivery().runRetryLoop()
        }
    }

    @Provides
    @IntoSet
    internal fun exportDurableConsequences(consequences: () -> EntryMergeDurableConsequences): InjektExport<*> =
        injektExport(consequences)

    @Provides
    @IntoSet
    internal fun exportLegacyConsequenceDelivery(
        delivery: () -> EntryMergeLegacyConsequenceDelivery,
    ): InjektExport<*> = injektExport(delivery)

    @Provides
    @IntoSet
    internal fun exportConsequenceDelivery(delivery: () -> EntryMergeConsequenceDelivery): InjektExport<*> =
        injektExport(delivery)

    @Provides
    @IntoSet
    fun exportFeature(feature: () -> EntryMergeFeature): InjektExport<*> = injektExport(feature)

    @Provides
    @IntoSet
    fun exportCandidateFeature(feature: () -> EntryMergeCandidateFeature): InjektExport<*> = injektExport(feature)

    @Provides
    @IntoSet
    fun exportNavigationFeature(feature: () -> EntryMergeNavigationFeature): InjektExport<*> = injektExport(feature)

    @Provides
    @IntoSet
    internal fun exportLibraryGroupingCoordinator(
        coordinator: () -> EntryMergeLibraryGroupingCoordinator,
    ): InjektExport<*> = injektExport(coordinator)

    @Provides
    @IntoSet
    fun exportLibraryGroupingFeature(feature: () -> EntryMergeLibraryGroupingFeature): InjektExport<*> =
        injektExport(feature)

    @Provides
    @IntoSet
    fun exportLibraryGroupingResolutionPort(port: () -> EntryLibraryGroupingResolutionPort): InjektExport<*> =
        injektExport(port)

    @Provides
    @IntoSet
    fun exportBackupFeature(feature: () -> EntryMergeBackupFeature): InjektExport<*> = injektExport(feature)

    @Provides
    @IntoSet
    internal fun exportBackupRestoreParticipation(
        participation: () -> EntryMergeBackupRestoreParticipation,
    ): InjektExport<*> = injektExport(participation)

    @Provides
    @IntoSet
    fun exportLibraryLifecycleFeature(feature: () -> EntryMergeLibraryLifecycleFeature): InjektExport<*> =
        injektExport(feature)

    @Provides
    @IntoSet
    fun exportMetadataRefreshFeature(feature: () -> EntryMergeMetadataRefreshFeature): InjektExport<*> =
        injektExport(feature)

    @Provides
    @IntoSet
    fun exportProfileMoveFeature(feature: () -> EntryMergeProfileMoveFeature): InjektExport<*> =
        injektExport(feature)

    @Provides
    @IntoSet
    fun exportConsequenceStatusFeature(feature: () -> EntryMergeConsequenceStatusFeature): InjektExport<*> =
        injektExport(feature)

    @Provides
    @IntoSet
    fun exportMigrationFeature(feature: () -> EntryMergeMigrationFeature): InjektExport<*> = injektExport(feature)

    @Provides
    @IntoSet
    internal fun exportChildOwnershipProjection(
        projection: () -> EntryMergeChildOwnershipProjection,
    ): InjektExport<*> = injektExport(projection)

    @Provides
    @IntoSet
    fun exportChildOwnershipResolutionPort(port: () -> EntryChildOwnershipResolutionPort): InjektExport<*> =
        injektExport(port)

    @Provides
    @IntoSet
    internal fun exportDownloadOwnershipProjection(
        projection: () -> EntryMergeDownloadOwnershipProjection,
    ): InjektExport<*> = injektExport(projection)
}

private fun EntryMergeProfileMoveExecutionResult.requireApplied() {
    check(this == EntryMergeProfileMoveExecutionResult.Applied) {
        "Merge Profile-move participation failed: $this"
    }
}
