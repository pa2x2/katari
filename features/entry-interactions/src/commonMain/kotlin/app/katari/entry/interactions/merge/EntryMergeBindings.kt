package app.katari.entry.interactions.merge

import app.katari.core.metro.AppCoroutineScope
import app.katari.core.metro.StartupWarmup
import app.katari.entry.interactions.download.EntryDownloadMaintenanceFeature
import app.katari.entry.interactions.durable.EntryDurableConsequences
import app.katari.entry.interactions.durable.EntryDurableParticipant
import app.katari.entry.interactions.library.membership.host.EntryLibraryCustomCoverHost
import app.katari.entry.interactions.lifecycle.metadata.EntryMetadataLifecycleFeature
import app.katari.entry.interactions.merge.backup.EntryMergeBackupParticipant
import app.katari.entry.interactions.merge.backup.EntryMergeBackupRestoreParticipation
import app.katari.entry.interactions.merge.backup.EntryMergeSourceListingBackup
import app.katari.entry.interactions.merge.backup.EntryMergeSourceListingBackupParticipant
import app.katari.entry.interactions.merge.consequence.EntryMergeConsequenceDelivery
import app.katari.entry.interactions.merge.consequence.EntryMergeDurableConsequences
import app.katari.entry.interactions.merge.consequence.EntryMergeDurableEvent
import app.katari.entry.interactions.merge.consequence.cover.EntryMergeCustomCoverParticipant
import app.katari.entry.interactions.merge.consequence.legacy.EntryMergeLegacyConsequenceDelivery
import app.katari.entry.interactions.merge.host.EntryMergeHost
import app.katari.entry.interactions.merge.source.EntryMergeSourceGroupCoordinator
import app.katari.entry.interactions.persistence.backup.EntryBackupStateParticipant
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
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
        libraryCustomCoverHost: EntryLibraryCustomCoverHost,
        downloadMaintenance: () -> EntryDownloadMaintenanceFeature,
    ): EntryMergeLegacyConsequenceDelivery = EntryMergeLegacyConsequenceDelivery(
        host = mergeHost,
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
        sourceGroups: EntryMergeSourceGroupCoordinator,
    ): EntryMergeFeature = EntryMergeWorkflowCoordinator(
        host = mergeHost,
        durableConsequences = durableConsequences,
        consequences = consequences,
        sourceGroups = sourceGroups,
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun sourceGroupCoordinator(
        mergeHost: EntryMergeHost,
        metadataLifecycle: () -> EntryMetadataLifecycleFeature,
    ): EntryMergeSourceGroupCoordinator = EntryMergeSourceGroupCoordinator(mergeHost, metadataLifecycle)

    @Provides
    internal fun sourceGroupFeature(coordinator: EntryMergeSourceGroupCoordinator): EntryMergeSourceGroupFeature =
        coordinator

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
    internal fun libraryLifecycleFeature(
        mergeHost: EntryMergeHost,
        sourceGroups: EntryMergeSourceGroupCoordinator,
    ): EntryMergeLibraryLifecycleFeature = EntryMergeLibraryLifecycleCoordinator(mergeHost, sourceGroups)

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

    @Provides
    @SingleIn(AppScope::class)
    internal fun sourceListingBackup(mergeHost: EntryMergeHost): EntryMergeSourceListingBackup =
        EntryMergeSourceListingBackup(mergeHost)

    @Provides
    @IntoSet
    internal fun sourceListingBackupState(
        listings: () -> EntryMergeSourceListingBackup,
    ): EntryBackupStateParticipant = EntryMergeSourceListingBackupParticipant(listings)
}
