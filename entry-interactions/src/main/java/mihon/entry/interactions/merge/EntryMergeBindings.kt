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
import mihon.core.metro.StartupWarmup
import mihon.entry.interactions.download.EntryDownloadMaintenanceFeature
import mihon.entry.interactions.durable.EntryDurableConsequences
import mihon.entry.interactions.durable.EntryDurableParticipant
import mihon.entry.interactions.library.membership.host.EntryLibraryCustomCoverHost
import mihon.entry.interactions.merge.backup.EntryMergeBackupParticipant
import mihon.entry.interactions.merge.backup.EntryMergeBackupRestoreParticipation
import mihon.entry.interactions.merge.consequence.EntryMergeConsequenceDelivery
import mihon.entry.interactions.merge.consequence.EntryMergeDurableConsequences
import mihon.entry.interactions.merge.consequence.EntryMergeDurableEvent
import mihon.entry.interactions.merge.consequence.cover.EntryMergeCustomCoverParticipant
import mihon.entry.interactions.merge.consequence.legacy.EntryMergeLegacyConsequenceDelivery
import mihon.entry.interactions.merge.host.EntryMergeHost
import mihon.entry.interactions.persistence.backup.EntryBackupStateParticipant
import mihon.entry.interactions.tracking.EntryTrackingFeature
import tachiyomi.domain.entry.service.EntryChildOwnershipResolutionPort
import tachiyomi.domain.entry.service.EntryLibraryGroupingResolutionPort

@ContributesTo(AppScope::class)
@BindingContainer
object EntryMergeBindings {
    @Provides
    @SingleIn(AppScope::class)
    internal fun durableConsequences(
        participants: Set<EntryDurableParticipant<EntryMergeDurableEvent>>,
    ): EntryMergeDurableConsequences = EntryMergeDurableConsequences(EntryDurableConsequences(participants))

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
        mergeHost: EntryMergeHost,
        durableConsequences: EntryMergeDurableConsequences,
        consequences: EntryMergeConsequenceDelivery,
    ): EntryMergeFeature = EntryMergeWorkflowCoordinator(
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
    internal fun customCoverParticipant(
        mergeHost: EntryMergeHost,
        libraryCustomCoverHost: EntryLibraryCustomCoverHost,
    ): EntryDurableParticipant<EntryMergeDurableEvent> = EntryMergeCustomCoverParticipant(
        mergeHost,
        libraryCustomCoverHost::cleanupAfterLibraryRemoval,
    )

    @Provides
    @IntoSet
    internal fun consequenceRetryWarmup(
        @AppCoroutineScope scope: CoroutineScope,
        delivery: () -> EntryMergeConsequenceDelivery,
    ): StartupWarmup = StartupWarmup {
        scope.launch {
            delivery().runRetryLoop()
        }
    }

    @Provides
    @IntoSet
    internal fun backupState(
        backupFeature: () -> EntryMergeBackupFeature,
        restoreParticipation: () -> EntryMergeBackupRestoreParticipation,
    ): EntryBackupStateParticipant = EntryMergeBackupParticipant(backupFeature, restoreParticipation)
}
