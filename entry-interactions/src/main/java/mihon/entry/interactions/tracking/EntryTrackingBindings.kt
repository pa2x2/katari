package mihon.entry.interactions.tracking

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.entry.interactions.lifecycle.profile.host.EntryProfileMoveTrackingStateHost
import mihon.entry.interactions.lifecycle.profile.stateRequest
import mihon.entry.interactions.merge.host.EntryMergeHost
import mihon.entry.interactions.persistence.backup.EntryBackupStateParticipant
import mihon.entry.interactions.tracking.backup.DefaultEntryTrackingBackupFeature
import mihon.entry.interactions.tracking.backup.EntryTrackingBackupFeature
import mihon.entry.interactions.tracking.backup.EntryTrackingBackupParticipant
import mihon.entry.interactions.tracking.host.EntryTrackingHost
import mihon.entry.interactions.tracking.lifecycle.ENTRY_TRACKING_PROFILE_MOVE_PARTICIPANT
import mihon.entry.interactions.tracking.lifecycle.EntryTrackingProfileMoveContributor
import mihon.entry.interactions.tracking.merge.EntryTrackingMergeContributor
import mihon.entry.interactions.tracking.merge.entryTrackingMergeBinding
import mihon.entry.interactions.tracking.migration.EntryTrackingMigrationContributor
import mihon.entry.interactions.tracking.migration.entryTrackingMigrationBinding
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.execution.FeatureDurableExecutionParticipantBinding
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
    fun migrationGraphContributor(): FeatureGraphContributor = EntryTrackingMigrationContributor

    @Provides
    @IntoSet
    fun mergeGraphContributor(): FeatureGraphContributor = EntryTrackingMergeContributor

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
    fun migrationParticipant(feature: () -> EntryTrackingFeature): FeatureExecutionParticipantBinding<*> =
        entryTrackingMigrationBinding(feature)

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

    @Provides
    @IntoSet
    internal fun backupState(
        backupFeature: () -> EntryTrackingBackupFeature,
    ): EntryBackupStateParticipant = EntryTrackingBackupParticipant(backupFeature)
}
