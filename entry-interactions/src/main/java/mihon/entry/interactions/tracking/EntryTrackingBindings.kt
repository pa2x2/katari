package mihon.entry.interactions.tracking

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.entry.interactions.lifecycle.profile.host.EntryProfileMoveTrackingStateHost
import mihon.entry.interactions.lifecycle.profile.stateRequest
import mihon.entry.interactions.media.session.EntryMediaSessionConsequence
import mihon.entry.interactions.media.session.EntryMediaSessionEvent
import mihon.entry.interactions.merge.host.EntryMergeHost
import mihon.entry.interactions.persistence.backup.decodeEntryBackupState
import mihon.entry.interactions.persistence.backup.entryBackupStateEnvelope
import mihon.entry.interactions.tracking.backup.DefaultEntryTrackingBackupFeature
import mihon.entry.interactions.tracking.backup.ENTRY_TRACKING_BACKUP_RESTORE_PARTICIPANT
import mihon.entry.interactions.tracking.backup.ENTRY_TRACKING_BACKUP_SCHEMA_VERSION
import mihon.entry.interactions.tracking.backup.ENTRY_TRACKING_BACKUP_SNAPSHOT_PARTICIPANT
import mihon.entry.interactions.tracking.backup.ENTRY_TRACKING_BACKUP_STATE_ID
import mihon.entry.interactions.tracking.backup.EntryTrackingBackupContributor
import mihon.entry.interactions.tracking.backup.EntryTrackingBackupFeature
import mihon.entry.interactions.tracking.backup.EntryTrackingBackupState
import mihon.entry.interactions.tracking.host.EntryTrackingHost
import mihon.entry.interactions.tracking.lifecycle.ENTRY_TRACKING_PROFILE_MOVE_PARTICIPANT
import mihon.entry.interactions.tracking.lifecycle.EntryTrackingProfileMoveContributor
import mihon.entry.interactions.tracking.merge.EntryTrackingMergeContributor
import mihon.entry.interactions.tracking.merge.entryTrackingMergeBinding
import mihon.entry.interactions.tracking.migration.EntryTrackingMigrationContributor
import mihon.entry.interactions.tracking.migration.entryTrackingMigrationBinding
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.contextEvidence
import mihon.feature.graph.execution.FeatureDurableExecutionParticipantBinding
import mihon.feature.graph.execution.FeatureExecutionContextResolver
import mihon.feature.graph.execution.FeatureExecutionHandler
import mihon.feature.graph.execution.FeatureExecutionParticipantBinding
import mihon.feature.runtime.FeatureRuntimeComposition
import tachiyomi.domain.entry.model.Entry

@ContributesTo(AppScope::class)
@BindingContainer
object EntryTrackingBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun feature(composition: FeatureRuntimeComposition, trackingHost: EntryTrackingHost): EntryTrackingFeature =
        DefaultEntryTrackingFeature(
            evaluation = composition.evaluation,
            host = trackingHost,
        )

    @Provides
    @SingleIn(AppScope::class)
    fun backupFeature(trackingHost: EntryTrackingHost): EntryTrackingBackupFeature =
        DefaultEntryTrackingBackupFeature(trackingHost.backup)

    @Provides
    @IntoSet
    fun graphContributor(): FeatureGraphContributor = EntryTrackingFeatureContributor

    @Provides
    @IntoSet
    fun libraryMembershipGraphContributor(): FeatureGraphContributor = EntryTrackingLibraryMembershipContributor

    @Provides
    @IntoSet
    fun profileMoveGraphContributor(): FeatureGraphContributor = EntryTrackingProfileMoveContributor

    @Provides
    @IntoSet
    fun backupGraphContributor(): FeatureGraphContributor = EntryTrackingBackupContributor

    @Provides
    @IntoSet
    fun migrationGraphContributor(): FeatureGraphContributor = EntryTrackingMigrationContributor

    @Provides
    @IntoSet
    fun mergeGraphContributor(): FeatureGraphContributor = EntryTrackingMergeContributor

    @Provides
    @IntoSet
    fun mediaSessionGraphContributor(): FeatureGraphContributor = EntryTrackingMediaSessionContributor

    @Provides
    @IntoSet
    fun mergeParticipant(
        mergeHost: EntryMergeHost,
        feature: () -> EntryTrackingFeature,
    ): FeatureDurableExecutionParticipantBinding<*> = entryTrackingMergeBinding(
        resolveEntry = { profileId, type, sourceId, url ->
            mergeHost.profile(profileId).resolveEntryIdentity(
                Entry.create().copy(
                    profileId = profileId,
                    type = type,
                    source = sourceId,
                    url = url,
                ),
            )
        },
        feature = feature,
    )

    @Provides
    @IntoSet
    fun mediaSessionParticipant(
        trackingHost: EntryTrackingHost,
        feature: () -> EntryTrackingFeature,
    ): FeatureExecutionParticipantBinding<*> = entryTrackingMediaSessionBinding(
        feature = feature,
        automaticSynchronizationEnabled = {
            trackingHost.automation.isAutomaticProgressSynchronizationEnabled()
        },
    )

    @Provides
    @IntoSet
    fun migrationParticipant(feature: () -> EntryTrackingFeature): FeatureExecutionParticipantBinding<*> =
        entryTrackingMigrationBinding(feature)

    @Provides
    @IntoSet
    fun backupSnapshotParticipant(
        backupFeature: () -> EntryTrackingBackupFeature,
    ): FeatureExecutionParticipantBinding<*> = FeatureExecutionParticipantBinding(
        definition = ENTRY_TRACKING_BACKUP_SNAPSHOT_PARTICIPANT,
        handler = FeatureExecutionHandler { event ->
            if (!event.selection.includeTrackingState) return@FeatureExecutionHandler
            val state =
                backupFeature().snapshot(event.profileId, event.entry)
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
    )

    @Provides
    @IntoSet
    fun backupRestoreParticipant(
        backupFeature: () -> EntryTrackingBackupFeature,
    ): FeatureExecutionParticipantBinding<*> = FeatureExecutionParticipantBinding(
        definition = ENTRY_TRACKING_BACKUP_RESTORE_PARTICIPANT,
        handler = FeatureExecutionHandler { event ->
            val state = event.states.decodeEntryBackupState(
                ENTRY_TRACKING_BACKUP_STATE_ID,
                ENTRY_TRACKING_BACKUP_SCHEMA_VERSION,
                EntryTrackingBackupState.serializer(),
            ) ?: return@FeatureExecutionHandler
            backupFeature().restore(event.profileId, event.entry, state)
        },
    )

    @Provides
    @IntoSet
    fun libraryAdditionParticipant(feature: () -> EntryTrackingFeature): FeatureExecutionParticipantBinding<*> =
        FeatureExecutionParticipantBinding(
            definition = ENTRY_TRACKING_LIBRARY_ADDITION_PARTICIPANT,
            handler = FeatureExecutionHandler { event ->
                feature().bindAutomatically(event.entry)
            },
        )

    @Provides
    @IntoSet
    fun profileMoveParticipant(
        profileMoveTrackingStateHost: EntryProfileMoveTrackingStateHost,
    ): FeatureExecutionParticipantBinding<*> = FeatureExecutionParticipantBinding(
        definition = ENTRY_TRACKING_PROFILE_MOVE_PARTICIPANT,
        handler = FeatureExecutionHandler { event ->
            profileMoveTrackingStateHost.move(event.plan.stateRequest())
        },
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
