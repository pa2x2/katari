package mihon.entry.interactions

import mihon.feature.graph.FeatureExecutionContextResolver
import mihon.feature.graph.FeatureExecutionHandler
import mihon.feature.graph.FeatureExecutionParticipantBinding
import mihon.feature.graph.contextEvidence
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

internal val EntryTrackingFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.tracking",
    contributor = EntryTrackingFeatureContributor,
    additionalContributors = listOf(
        EntryTrackingLibraryMembershipContributor,
        EntryTrackingProfileMoveContributor,
        EntryTrackingBackupContributor,
        EntryTrackingMigrationContributor,
        EntryTrackingMergeContributor,
        EntryTrackingMediaSessionContributor,
    ),
) { context ->
    addSingletonFactory<EntryTrackingFeature> {
        DefaultEntryTrackingFeature(
            evaluation = get<EntryInteractionComposition>().featureGraphEvaluation,
            host = context.dependencies.trackingHost,
        )
    }
    addSingletonFactory<EntryTrackingBackupFeature> {
        DefaultEntryTrackingBackupFeature(context.dependencies.trackingHost.backup)
    }
    EntryFeatureRuntimeArtifacts(
        durableExecutionBindings = listOf(
            entryTrackingMergeBinding(
                resolveEntry = { profileId, type, sourceId, url ->
                    context.dependencies.mergeHost.profile(profileId).resolveEntryIdentity(
                        tachiyomi.domain.entry.model.Entry.create().copy(
                            profileId = profileId,
                            type = type,
                            source = sourceId,
                            url = url,
                        ),
                    )
                },
                feature = { get<EntryTrackingFeature>() },
            ),
        ),
        executionBindings = listOf(
            entryTrackingMediaSessionBinding(
                feature = { get<EntryTrackingFeature>() },
                automaticSynchronizationEnabled = {
                    context.dependencies.trackingHost.automation.isAutomaticProgressSynchronizationEnabled()
                },
            ),
            entryTrackingMigrationBinding { get<EntryTrackingFeature>() },
            FeatureExecutionParticipantBinding(
                definition = ENTRY_TRACKING_BACKUP_SNAPSHOT_PARTICIPANT,
                handler = FeatureExecutionHandler { event ->
                    if (!event.selection.includeTrackingState) return@FeatureExecutionHandler
                    val state = get<EntryTrackingBackupFeature>().snapshot(event.profileId, event.entry)
                        ?: return@FeatureExecutionHandler
                    event.contributions.add(
                        entryBackupStateEnvelope(
                            ENTRY_TRACKING_BACKUP_STATE_ID,
                            ENTRY_TRACKING_BACKUP_SCHEMA_VERSION,
                            EntryTrackingBackupState.serializer(),
                            state,
                        ),
                    )
                },
            ),
            FeatureExecutionParticipantBinding(
                definition = ENTRY_TRACKING_BACKUP_RESTORE_PARTICIPANT,
                handler = FeatureExecutionHandler { event ->
                    val state = event.states.decodeEntryBackupState(
                        ENTRY_TRACKING_BACKUP_STATE_ID,
                        ENTRY_TRACKING_BACKUP_SCHEMA_VERSION,
                        EntryTrackingBackupState.serializer(),
                    ) ?: return@FeatureExecutionHandler
                    get<EntryTrackingBackupFeature>().restore(event.profileId, event.entry, state)
                },
            ),
            FeatureExecutionParticipantBinding(
                definition = ENTRY_TRACKING_LIBRARY_ADDITION_PARTICIPANT,
                handler = FeatureExecutionHandler { event ->
                    get<EntryTrackingFeature>().bindAutomatically(event.entry)
                },
            ),
            FeatureExecutionParticipantBinding(
                definition = ENTRY_TRACKING_PROFILE_MOVE_PARTICIPANT,
                handler = FeatureExecutionHandler { event ->
                    context.dependencies.profileMoveTrackingStateHost.move(event.plan.stateRequest())
                },
            ),
        ),
        runtimeBoundaries = listOf(
            entryFeatureRuntimeBoundary { get<EntryTrackingFeature>() },
            entryFeatureRuntimeBoundary { get<EntryTrackingBackupFeature>() },
        ),
    )
}

internal fun entryTrackingMediaSessionBinding(
    feature: () -> EntryTrackingFeature,
    automaticSynchronizationEnabled: () -> Boolean,
) = FeatureExecutionParticipantBinding(
    definition = ENTRY_TRACKING_MEDIA_SESSION_PARTICIPANT,
    contextResolver = FeatureExecutionContextResolver { execution ->
        listOf(
            contextEvidence(
                ENTRY_MEDIA_SESSION_TRACKING_ALLOWED,
                execution.permits(EntryMediaSessionConsequence.SYNCHRONIZE_TRACKING),
            ),
        )
    },
    handler = FeatureExecutionHandler { execution ->
        val event = execution.event as? EntryMediaSessionEvent.Progressed
            ?: return@FeatureExecutionHandler
        if (execution.progressResult?.completedNow != true) return@FeatureExecutionHandler
        if (event.child.chapterNumber < 0.0) return@FeatureExecutionHandler
        if (!automaticSynchronizationEnabled()) return@FeatureExecutionHandler
        feature().synchronizeProgress(
            entry = event.visibleEntry,
            progress = event.child.chapterNumber,
        )
    },
)
