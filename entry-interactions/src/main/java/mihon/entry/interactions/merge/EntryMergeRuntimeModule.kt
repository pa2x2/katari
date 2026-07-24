package mihon.entry.interactions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import mihon.feature.graph.FeatureExecutionHandler
import mihon.feature.graph.FeatureExecutionParticipantBinding
import tachiyomi.domain.entry.service.EntryChildOwnershipResolutionPort
import tachiyomi.domain.entry.service.EntryLibraryGroupingResolutionPort
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

internal val EntryMergeFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.merge",
    contributor = EntryMergeFeatureContributor,
    additionalContributors = listOf(
        EntryMergeLibraryMembershipContributor,
        EntryMergeDestructiveRemovalContributor,
        EntryMergeProfileMoveContributor,
        EntryMergeBackupContributor,
        EntryMergeCustomCoverContributor,
    ),
) { context ->
    val dependencies = context.dependencies
    addSingletonFactory {
        EntryMergeDurableConsequences(get<EntryInteractionComposition>().featureExecutions)
    }
    addSingletonFactory {
        EntryMergeLegacyConsequenceDelivery(
            host = dependencies.mergeHost,
            tracking = { get() },
            coverCleanup = dependencies.mergeCoverCleanup,
            downloadMaintenance = { get() },
        )
    }
    addSingletonFactory {
        EntryMergeConsequenceDelivery(
            host = dependencies.mergeHost,
            consequences = get(),
            legacy = get(),
        )
    }
    addSingletonFactory<EntryMergeFeature> {
        EntryMergeWorkflowCoordinator(
            evaluation = get<EntryInteractionComposition>().featureGraphEvaluation,
            host = dependencies.mergeHost,
            durableConsequences = get<EntryMergeDurableConsequences>(),
            consequences = get(),
        )
    }
    addSingletonFactory<EntryMergeCandidateFeature> { EntryMergeCandidateCoordinator(dependencies.mergeHost) }
    addSingletonFactory<EntryMergeNavigationFeature> { EntryMergeNavigationCoordinator(dependencies.mergeHost) }
    addSingletonFactory { EntryMergeLibraryGroupingCoordinator(dependencies.mergeHost) }
    addSingletonFactory<EntryMergeLibraryGroupingFeature> { get<EntryMergeLibraryGroupingCoordinator>() }
    addSingletonFactory<EntryLibraryGroupingResolutionPort> { get<EntryMergeLibraryGroupingCoordinator>() }
    addSingletonFactory<EntryMergeBackupFeature> { EntryMergeBackupCoordinator(dependencies.mergeHost) }
    addSingletonFactory { EntryMergeBackupRestoreParticipation(get<EntryMergeBackupFeature>()) }
    addSingletonFactory<EntryMergeLibraryLifecycleFeature> {
        EntryMergeLibraryLifecycleCoordinator(dependencies.mergeHost)
    }
    addSingletonFactory<EntryMergeMetadataRefreshFeature> {
        EntryMergeMetadataRefreshCoordinator(dependencies.mergeHost)
    }
    addSingletonFactory<EntryMergeProfileMoveFeature> { EntryMergeProfileMoveCoordinator(dependencies.mergeHost) }
    addSingletonFactory<EntryMergeConsequenceStatusFeature> {
        EntryMergeConsequenceStatusCoordinator(dependencies.mergeHost, get())
    }
    addSingletonFactory<EntryMergeMigrationFeature> { EntryMergeMigrationCoordinator(dependencies.mergeHost) }
    addSingletonFactory<EntryMergeChildOwnershipProjection> {
        EntryMergeChildOwnershipCoordinator(dependencies.mergeHost)
    }
    addSingletonFactory<EntryChildOwnershipResolutionPort> { get<EntryMergeChildOwnershipProjection>() }
    addSingletonFactory<EntryMergeDownloadOwnershipProjection> {
        EntryMergeDownloadOwnershipCoordinator(dependencies.mergeHost)
    }
    EntryFeatureRuntimeArtifacts(
        durableExecutionBindings = listOf(
            entryMergeCustomCoverBinding(dependencies.mergeHost, dependencies.mergeCoverCleanup),
        ),
        executionBindings = listOf(
            FeatureExecutionParticipantBinding(
                definition = ENTRY_MERGE_BACKUP_SNAPSHOT_PARTICIPANT,
                handler = FeatureExecutionHandler { event ->
                    val state = get<EntryMergeBackupFeature>().snapshotForBackup(
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
            ),
            FeatureExecutionParticipantBinding(
                definition = ENTRY_MERGE_BACKUP_RESTORE_PARTICIPANT,
                handler = FeatureExecutionHandler { event ->
                    val state = event.states.decodeEntryBackupState(
                        ENTRY_MERGE_BACKUP_STATE_ID,
                        ENTRY_MERGE_BACKUP_SCHEMA_VERSION,
                        EntryMergeBackupMember.serializer(),
                    ) ?: return@FeatureExecutionHandler
                    get<EntryMergeBackupRestoreParticipation>().enqueue(event, state)
                },
            ),
            FeatureExecutionParticipantBinding(
                definition = ENTRY_MERGE_BACKUP_FINALIZE_PARTICIPANT,
                handler = FeatureExecutionHandler { event ->
                    get<EntryMergeBackupRestoreParticipation>().finalize(event)
                },
            ),
            FeatureExecutionParticipantBinding(
                definition = ENTRY_MERGE_LIBRARY_REMOVAL_PARTICIPANT,
                handler = FeatureExecutionHandler { event ->
                    val result = get<EntryMergeLibraryLifecycleFeature>().entriesRemovedFromLibrary(event.entries)
                    check(result.unresolvedGroupCount == 0) {
                        "Merge membership changed while removing Library entries"
                    }
                },
            ),
            FeatureExecutionParticipantBinding(
                definition = ENTRY_MERGE_DESTRUCTIVE_REMOVAL_PARTICIPANT,
                handler = FeatureExecutionHandler { event ->
                    val result = get<EntryMergeLibraryLifecycleFeature>().entriesRemovedFromLibrary(event.entries)
                    check(result.unresolvedGroupCount == 0) {
                        "Merge membership changed during destructive Entry removal"
                    }
                },
            ),
            FeatureExecutionParticipantBinding(
                definition = ENTRY_MERGE_PROFILE_MOVE_PREPARATION_PARTICIPANT,
                handler = FeatureExecutionHandler { event ->
                    when (
                        val result = get<EntryMergeProfileMoveFeature>().prepare(
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
            ),
            FeatureExecutionParticipantBinding(
                definition = ENTRY_MERGE_PROFILE_MOVE_DESTINATION_PARTICIPANT,
                handler = FeatureExecutionHandler { event ->
                    val reference = event.contributions.participantReference(
                        ENTRY_MERGE_PROFILE_MOVE_PARTICIPANT_ID,
                        event.type,
                    ) as? EntryMergeProfileMoveReference ?: return@FeatureExecutionHandler
                    when (
                        val result = get<EntryMergeProfileMoveFeature>().inspectDestination(
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
            ),
            FeatureExecutionParticipantBinding(
                definition = ENTRY_MERGE_PROFILE_MOVING_PARTICIPANT,
                handler = FeatureExecutionHandler { event ->
                    val reference = event.reference.mergeReference(event.type) ?: return@FeatureExecutionHandler
                    get<EntryMergeProfileMoveFeature>().begin(event.plan.toMergeIntent(reference)).requireApplied()
                },
            ),
            FeatureExecutionParticipantBinding(
                definition = ENTRY_MERGE_PROFILE_STATE_MOVED_PARTICIPANT,
                handler = FeatureExecutionHandler { event ->
                    val reference = event.reference.mergeReference(event.type) ?: return@FeatureExecutionHandler
                    get<EntryMergeProfileMoveFeature>().complete(event.plan.toMergeIntent(reference)).requireApplied()
                },
            ),
        ),
        runtimeBoundaries = listOf(
            entryFeatureRuntimeBoundary { get<EntryMergeFeature>() },
            entryFeatureRuntimeBoundary { get<EntryMergeChildOwnershipProjection>() },
            entryFeatureRuntimeBoundary { get<EntryMergeDownloadOwnershipProjection>() },
            entryFeatureRuntimeBoundary { get<EntryLibraryGroupingResolutionPort>() },
        ),
        warmups = listOf {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                get<EntryMergeConsequenceDelivery>().runRetryLoop()
            }
        },
    )
}

private fun EntryMergeProfileMoveExecutionResult.requireApplied() {
    check(this == EntryMergeProfileMoveExecutionResult.Applied) {
        "Merge Profile-move participation failed: $this"
    }
}
