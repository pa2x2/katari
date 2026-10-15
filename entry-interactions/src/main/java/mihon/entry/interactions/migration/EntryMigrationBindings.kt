package mihon.entry.interactions.migration

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
import mihon.entry.interactions.durable.EntryDurableConsequences
import mihon.entry.interactions.durable.EntryDurableParticipant
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
import mihon.entry.interactions.tracking.EntryTrackingFeature
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.runtime.FeatureRuntimeComposition
import mihon.feature.runtime.FeatureRuntimeWarmup

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
        composition: FeatureRuntimeComposition,
        preparationHost: EntryMigrationPreparationHost,
        executionHost: EntryMigrationExecutionHost,
        sourceRefresh: EntrySourceRefreshFeature,
        mergeMigration: EntryMergeMigrationFeature,
        downloads: () -> EntryDownloadMaintenanceFeature,
        tracking: () -> EntryTrackingFeature,
        durableConsequences: EntryDurableConsequences<EntryMigrationDurableEvent>,
        consequences: EntryMigrationConsequenceDelivery,
    ): EntryMigrationFeature = DefaultEntryMigrationFeature(
        evaluation = composition.evaluation,
        preparationHost = preparationHost,
        executionHost = executionHost,
        sourceRefresh = sourceRefresh,
        mergeMigration = mergeMigration,
        downloads = downloads,
        tracking = tracking,
        durableConsequences = durableConsequences,
        consequences = consequences,
    )

    @Provides
    @IntoSet
    fun graphContributor(): FeatureGraphContributor = EntryMigrationFeatureContributor

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
    ): FeatureRuntimeWarmup = FeatureRuntimeWarmup {
        scope.launch {
            delivery().runRetryLoop()
        }
    }
}
