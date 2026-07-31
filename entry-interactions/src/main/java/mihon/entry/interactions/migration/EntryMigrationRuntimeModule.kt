package mihon.entry.interactions.migration

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import mihon.entry.interactions.migration.consequence.EntryMigrationConsequenceDelivery
import mihon.entry.interactions.migration.consequence.EntryMigrationConsequenceStatusCoordinator
import mihon.entry.interactions.migration.consequence.EntryMigrationDurableConsequences
import mihon.entry.interactions.migration.consequence.cover.EntryMigrationCustomCoverContributor
import mihon.entry.interactions.migration.consequence.cover.EntryMigrationCustomCoverOrphanCleanup
import mihon.entry.interactions.migration.consequence.cover.entryMigrationCustomCoverBinding
import mihon.entry.interactions.migration.options.EntryMigrationOptionDiscovery
import mihon.entry.interactions.migration.preparation.EntryMigrationTransitionPreparation
import mihon.entry.interactions.runtime.production.EntryFeatureRuntimeArtifacts
import mihon.entry.interactions.runtime.production.EntryFeatureRuntimeModule
import mihon.entry.interactions.runtime.production.entryFeatureRuntimeBoundary
import mihon.feature.runtime.FeatureRuntimeComposition
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

internal val EntryMigrationFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.migration",
    contributor = EntryMigrationFeatureContributor,
    additionalContributors = listOf(EntryMigrationCustomCoverContributor),
) { context ->
    val dependencies = context.dependencies
    addSingletonFactory {
        EntryMigrationDurableConsequences(get<FeatureRuntimeComposition>().executions)
    }
    addSingletonFactory {
        EntryMigrationOptionDiscovery(get<FeatureRuntimeComposition>().executions)
    }
    addSingletonFactory {
        EntryMigrationTransitionPreparation(get<FeatureRuntimeComposition>().executions)
    }
    addSingletonFactory {
        EntryMigrationCustomCoverOrphanCleanup(
            consequenceHost = dependencies.migrationConsequenceHost,
            coverHost = dependencies.migrationCustomCoverHost,
        )
    }
    addSingletonFactory {
        EntryMigrationConsequenceDelivery(
            host = dependencies.migrationConsequenceHost,
            consequences = get(),
            coverOrphanCleanup = get(),
        )
    }
    addSingletonFactory<EntryMigrationConsequenceStatusFeature> {
        EntryMigrationConsequenceStatusCoordinator(dependencies.migrationConsequenceHost, get())
    }
    addSingletonFactory<EntryMigrationFeature> {
        DefaultEntryMigrationFeature(
            evaluation = get<FeatureRuntimeComposition>().evaluation,
            preparationHost = dependencies.migrationPreparationHost,
            executionHost = dependencies.migrationExecutionHost,
            sourceRefresh = get(),
            mergeMigration = get(),
            optionDiscovery = get(),
            transitionPreparation = get(),
            durableConsequences = get(),
            consequences = get(),
        )
    }
    EntryFeatureRuntimeArtifacts(
        durableExecutionBindings = listOf(
            entryMigrationCustomCoverBinding(dependencies.migrationCustomCoverHost),
        ),
        runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntryMigrationFeature>() }),
        warmups = listOf {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                get<EntryMigrationConsequenceDelivery>().runRetryLoop()
            }
        },
    )
}
