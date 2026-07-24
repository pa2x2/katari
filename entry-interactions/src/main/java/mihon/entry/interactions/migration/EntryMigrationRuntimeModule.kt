package mihon.entry.interactions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

internal val EntryMigrationFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.migration",
    contributor = EntryMigrationFeatureContributor,
    additionalContributors = listOf(EntryMigrationCustomCoverContributor),
) { context ->
    val dependencies = context.dependencies
    addSingletonFactory {
        EntryMigrationDurableConsequences(get<EntryInteractionComposition>().featureExecutions)
    }
    addSingletonFactory {
        EntryMigrationOptionDiscovery(get<EntryInteractionComposition>().featureExecutions)
    }
    addSingletonFactory {
        EntryMigrationTransitionPreparation(get<EntryInteractionComposition>().featureExecutions)
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
            evaluation = get<EntryInteractionComposition>().featureGraphEvaluation,
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
