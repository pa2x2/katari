package mihon.entry.interactions.migration

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mihon.core.metro.AppCoroutineScope
import mihon.core.metro.StartupWarmup
import mihon.entry.interactions.download.EntryDownloadMaintenanceFeature
import mihon.entry.interactions.durable.EntryDurableConsequences
import mihon.entry.interactions.durable.EntryDurableParticipant
import mihon.entry.interactions.merge.EntryMergeLibraryLifecycleFeature
import mihon.entry.interactions.merge.EntryMergeMigrationFeature
import mihon.entry.interactions.migration.consequence.EntryMigrationConsequenceDelivery
import mihon.entry.interactions.migration.consequence.EntryMigrationConsequenceStatusCoordinator
import mihon.entry.interactions.migration.consequence.EntryMigrationDurableEvent
import mihon.entry.interactions.migration.consequence.cover.EntryMigrationCustomCoverOrphanCleanup
import mihon.entry.interactions.migration.consequence.cover.EntryMigrationCustomCoverParticipant
import mihon.entry.interactions.migration.host.EntryMigrationConsequenceHost
import mihon.entry.interactions.migration.host.EntryMigrationCustomCoverHost
import mihon.entry.interactions.migration.host.EntryMigrationExecutionHost
import mihon.entry.interactions.migration.host.EntryMigrationPreparationHost
import mihon.entry.interactions.source.EntrySourceRefreshFeature
import mihon.entry.interactions.state.EntryBookmarkProcessor
import mihon.entry.interactions.state.EntryConsumptionProcessor
import mihon.entry.interactions.state.EntryMigrationProvider
import mihon.entry.interactions.tracking.EntryTrackingFeature

@ContributesTo(AppScope::class)
@BindingContainer
object EntryMigrationBindings {
    @Provides
    @SingleIn(AppScope::class)
    internal fun durableConsequences(
        participants: Set<EntryDurableParticipant<EntryMigrationDurableEvent>>,
    ): EntryDurableConsequences<EntryMigrationDurableEvent> = EntryDurableConsequences(participants)

    @Provides
    @SingleIn(AppScope::class)
    internal fun customCoverOrphanCleanup(
        consequenceHost: EntryMigrationConsequenceHost,
        customCoverHost: EntryMigrationCustomCoverHost,
    ): EntryMigrationCustomCoverOrphanCleanup = EntryMigrationCustomCoverOrphanCleanup(
        consequenceHost = consequenceHost,
        coverHost = customCoverHost,
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun consequenceDelivery(
        consequenceHost: EntryMigrationConsequenceHost,
        consequences: EntryDurableConsequences<EntryMigrationDurableEvent>,
        coverOrphanCleanup: EntryMigrationCustomCoverOrphanCleanup,
    ): EntryMigrationConsequenceDelivery = EntryMigrationConsequenceDelivery(
        host = consequenceHost,
        consequences = consequences,
        coverOrphanCleanup = coverOrphanCleanup,
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun consequenceStatusFeature(
        consequenceHost: EntryMigrationConsequenceHost,
        delivery: EntryMigrationConsequenceDelivery,
    ): EntryMigrationConsequenceStatusFeature = EntryMigrationConsequenceStatusCoordinator(consequenceHost, delivery)

    @Provides
    @SingleIn(AppScope::class)
    internal fun feature(
        migrationProviders: Map<EntryType, EntryMigrationProvider>,
        consumptionProcessors: Map<EntryType, EntryConsumptionProcessor>,
        bookmarkProcessors: Map<EntryType, EntryBookmarkProcessor>,
        preparationHost: EntryMigrationPreparationHost,
        executionHost: EntryMigrationExecutionHost,
        sourceRefresh: EntrySourceRefreshFeature,
        mergeMigration: EntryMergeMigrationFeature,
        mergeLibraryLifecycle: () -> EntryMergeLibraryLifecycleFeature,
        downloads: () -> EntryDownloadMaintenanceFeature,
        tracking: () -> EntryTrackingFeature,
        durableConsequences: EntryDurableConsequences<EntryMigrationDurableEvent>,
        consequences: EntryMigrationConsequenceDelivery,
    ): EntryMigrationFeature = DefaultEntryMigrationFeature(
        types = EntryMigrationTypes(
            migration = migrationProviders.keys,
            consumption = migrationProviders.keys intersect consumptionProcessors.keys,
            bookmark = migrationProviders.keys intersect bookmarkProcessors.keys,
            childStateOption = migrationProviders.keys intersect (consumptionProcessors.keys + bookmarkProcessors.keys),
        ),
        preparationHost = preparationHost,
        executionHost = executionHost,
        sourceRefresh = sourceRefresh,
        mergeMigration = mergeMigration,
        mergeLibraryLifecycle = mergeLibraryLifecycle,
        downloads = downloads,
        tracking = tracking,
        durableConsequences = durableConsequences,
        consequences = consequences,
    )

    @Provides
    @IntoSet
    internal fun customCoverParticipant(
        customCoverHost: EntryMigrationCustomCoverHost,
    ): EntryDurableParticipant<EntryMigrationDurableEvent> = EntryMigrationCustomCoverParticipant(customCoverHost)

    @Provides
    @IntoSet
    internal fun consequenceRetryWarmup(
        @AppCoroutineScope scope: CoroutineScope,
        delivery: () -> EntryMigrationConsequenceDelivery,
    ): StartupWarmup = StartupWarmup {
        scope.launch {
            delivery().runRetryLoop()
        }
    }
}
