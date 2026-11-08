package app.katari.entry.interactions.migration

import app.katari.core.metro.AppCoroutineScope
import app.katari.core.metro.StartupWarmup
import app.katari.entry.interactions.download.EntryDownloadMaintenanceFeature
import app.katari.entry.interactions.durable.EntryDurableConsequences
import app.katari.entry.interactions.durable.EntryDurableParticipant
import app.katari.entry.interactions.merge.EntryMergeLibraryLifecycleFeature
import app.katari.entry.interactions.merge.EntryMergeMigrationFeature
import app.katari.entry.interactions.migration.consequence.EntryMigrationConsequenceDelivery
import app.katari.entry.interactions.migration.consequence.EntryMigrationConsequenceStatusCoordinator
import app.katari.entry.interactions.migration.consequence.EntryMigrationDurableEvent
import app.katari.entry.interactions.migration.consequence.cover.EntryMigrationCustomCoverOrphanCleanup
import app.katari.entry.interactions.migration.consequence.cover.EntryMigrationCustomCoverParticipant
import app.katari.entry.interactions.migration.host.EntryMigrationConsequenceHost
import app.katari.entry.interactions.migration.host.EntryMigrationCustomCoverHost
import app.katari.entry.interactions.migration.host.EntryMigrationExecutionHost
import app.katari.entry.interactions.migration.host.EntryMigrationPreparationHost
import app.katari.entry.interactions.source.EntrySourceRefreshFeature
import app.katari.entry.interactions.state.EntryBookmarkProcessor
import app.katari.entry.interactions.state.EntryConsumptionProcessor
import app.katari.entry.interactions.state.EntryMigrationProvider
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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
