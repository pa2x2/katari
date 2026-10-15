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
import mihon.entry.interactions.merge.EntryMergeMigrationFeature
import mihon.entry.interactions.migration.consequence.EntryMigrationConsequenceDelivery
import mihon.entry.interactions.migration.consequence.EntryMigrationConsequenceStatusCoordinator
import mihon.entry.interactions.migration.consequence.EntryMigrationDurableConsequences
import mihon.entry.interactions.migration.consequence.cover.EntryMigrationCustomCoverContributor
import mihon.entry.interactions.migration.consequence.cover.EntryMigrationCustomCoverOrphanCleanup
import mihon.entry.interactions.migration.consequence.cover.entryMigrationCustomCoverBinding
import mihon.entry.interactions.migration.host.EntryMigrationConsequenceHost
import mihon.entry.interactions.migration.host.EntryMigrationCustomCoverHost
import mihon.entry.interactions.migration.host.EntryMigrationExecutionHost
import mihon.entry.interactions.migration.host.EntryMigrationPreparationHost
import mihon.entry.interactions.migration.options.EntryMigrationOptionDiscovery
import mihon.entry.interactions.migration.preparation.EntryMigrationTransitionPreparation
import mihon.entry.interactions.source.EntrySourceRefreshFeature
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.execution.FeatureDurableExecutionParticipantBinding
import mihon.feature.runtime.FeatureRuntimeComposition
import mihon.feature.runtime.FeatureRuntimeWarmup

@ContributesTo(AppScope::class)
@BindingContainer
object EntryMigrationBindings {
    @Provides
    @SingleIn(AppScope::class)
    internal fun durableConsequences(composition: FeatureRuntimeComposition): EntryMigrationDurableConsequences =
        EntryMigrationDurableConsequences(composition.executions)

    @Provides
    @SingleIn(AppScope::class)
    internal fun optionDiscovery(composition: FeatureRuntimeComposition): EntryMigrationOptionDiscovery =
        EntryMigrationOptionDiscovery(composition.executions)

    @Provides
    @SingleIn(AppScope::class)
    internal fun transitionPreparation(composition: FeatureRuntimeComposition): EntryMigrationTransitionPreparation =
        EntryMigrationTransitionPreparation(composition.executions)

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
        consequences: EntryMigrationDurableConsequences,
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
        optionDiscovery: EntryMigrationOptionDiscovery,
        transitionPreparation: EntryMigrationTransitionPreparation,
        durableConsequences: EntryMigrationDurableConsequences,
        consequences: EntryMigrationConsequenceDelivery,
    ): EntryMigrationFeature = DefaultEntryMigrationFeature(
        evaluation = composition.evaluation,
        preparationHost = preparationHost,
        executionHost = executionHost,
        sourceRefresh = sourceRefresh,
        mergeMigration = mergeMigration,
        optionDiscovery = optionDiscovery,
        transitionPreparation = transitionPreparation,
        durableConsequences = durableConsequences,
        consequences = consequences,
    )

    @Provides
    @IntoSet
    fun graphContributor(): FeatureGraphContributor = EntryMigrationFeatureContributor

    @Provides
    @IntoSet
    fun customCoverGraphContributor(): FeatureGraphContributor = EntryMigrationCustomCoverContributor

    @Provides
    @IntoSet
    fun customCoverParticipant(
        customCoverHost: EntryMigrationCustomCoverHost,
    ): FeatureDurableExecutionParticipantBinding<*> = entryMigrationCustomCoverBinding(customCoverHost)

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
