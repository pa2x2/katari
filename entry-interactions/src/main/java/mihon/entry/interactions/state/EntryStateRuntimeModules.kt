package mihon.entry.interactions

import mihon.feature.graph.FeatureExecutionContextResolver
import mihon.feature.graph.FeatureExecutionHandler
import mihon.feature.graph.FeatureExecutionParticipantBinding
import mihon.feature.graph.contextEvidence
import tachiyomi.domain.library.service.LibraryPreferences
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

internal val EntryConsumptionFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.consumption",
    contributor = EntryConsumptionFeatureContributor,
) {
    addSingletonFactory<EntryConsumptionFeature> {
        val composition = get<EntryInteractionComposition>()
        DefaultEntryConsumptionFeature(
            evaluation = composition.featureGraphEvaluation,
            interaction = composition.interactions.consumption,
            downloadLifecycle = get(),
        )
    }
    EntryFeatureRuntimeArtifacts(
        runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntryConsumptionFeature>() }),
    )
}

internal val EntryBookmarkFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.bookmarking",
    contributor = EntryBookmarkFeatureContributor,
) {
    addSingletonFactory<EntryBookmarkFeature> {
        val composition = get<EntryInteractionComposition>()
        DefaultEntryBookmarkFeature(
            evaluation = composition.featureGraphEvaluation,
            interaction = composition.interactions.bookmark,
        )
    }
    EntryFeatureRuntimeArtifacts(
        runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntryBookmarkFeature>() }),
    )
}

internal val EntryUpdateEligibilityFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.update-eligibility",
    contributor = EntryUpdateEligibilityFeatureContributor,
) {
    addSingletonFactory<EntryUpdateEligibilityFeature> {
        val composition = get<EntryInteractionComposition>()
        val preferences = get<LibraryPreferences>()
        DefaultEntryUpdateEligibilityFeature(
            evaluation = composition.featureGraphEvaluation,
            currentPolicy = {
                preferences.autoUpdateEntryRestrictions.get().toEntryUpdateEligibilityPolicy()
            },
        )
    }
    EntryFeatureRuntimeArtifacts(
        runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntryUpdateEligibilityFeature>() }),
    )
}

internal val EntryProgressFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.progress-transfer",
    contributor = EntryProgressFeatureContributor,
    additionalContributors = listOf(
        EntryProgressBackupContributor,
        EntryProgressMigrationContributor,
        EntryProgressMediaSessionContributor,
    ),
) {
    addSingletonFactory<EntryProgressFeature> {
        val composition = get<EntryInteractionComposition>()
        DefaultEntryProgressFeature(
            evaluation = composition.featureGraphEvaluation,
            interaction = composition.interactions.progress,
            repository = get(),
            getEntryWithChapters = get(),
            globalLibraryPreferences = get(),
        )
    }
    EntryFeatureRuntimeArtifacts(
        durableExecutionBindings = listOf(
            entryProgressMigrationBinding { get<EntryProgressFeature>() },
        ),
        executionBindings = listOf(
            entryProgressMediaSessionBinding { get<EntryProgressFeature>() },
            FeatureExecutionParticipantBinding(
                definition = ENTRY_PROGRESS_BACKUP_SNAPSHOT_PARTICIPANT,
                handler = FeatureExecutionHandler { event ->
                    if (!event.selection.includeContentState) return@FeatureExecutionHandler
                    val result = get<EntryProgressFeature>().snapshot(event.entry)
                    if (result is EntryProgressSnapshotResult.Available && result.snapshot.states.isNotEmpty()) {
                        event.contributions.add(
                            entryBackupStateEnvelope(
                                ENTRY_PROGRESS_BACKUP_STATE_ID,
                                ENTRY_PROGRESS_BACKUP_SCHEMA_VERSION,
                                EntryProgressSnapshot.serializer(),
                                result.snapshot,
                            ),
                        )
                    }
                },
            ),
            FeatureExecutionParticipantBinding(
                definition = ENTRY_PROGRESS_BACKUP_RESTORE_PARTICIPANT,
                handler = FeatureExecutionHandler { event ->
                    val state = event.states.decodeEntryBackupState(
                        ENTRY_PROGRESS_BACKUP_STATE_ID,
                        ENTRY_PROGRESS_BACKUP_SCHEMA_VERSION,
                        EntryProgressSnapshot.serializer(),
                    ) ?: return@FeatureExecutionHandler
                    get<EntryProgressFeature>().restore(event.entry, state)
                },
            ),
        ),
        runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntryProgressFeature>() }),
    )
}

internal fun entryProgressMediaSessionBinding(
    feature: () -> EntryProgressFeature,
) = FeatureExecutionParticipantBinding(
    definition = ENTRY_PROGRESS_MEDIA_SESSION_PARTICIPANT,
    contextResolver = FeatureExecutionContextResolver { execution ->
        listOf(
            contextEvidence(
                ENTRY_MEDIA_SESSION_PROGRESS_ALLOWED,
                execution.permits(EntryMediaSessionConsequence.RECORD_PROGRESS),
            ),
        )
    },
    handler = FeatureExecutionHandler { execution ->
        val event = execution.event as? EntryMediaSessionEvent.Progressed
            ?: return@FeatureExecutionHandler
        execution.progressResult = feature().recordMediaProgress(event)
    },
)

internal fun Set<String>.toEntryUpdateEligibilityPolicy(): EntryUpdateEligibilityPolicy {
    return EntryUpdateEligibilityPolicy(
        skipCompleted = LibraryPreferences.ENTRY_NON_COMPLETED in this,
        skipWhenUnconsumed = LibraryPreferences.ENTRY_HAS_UNCONSUMED in this,
        skipWhenNotStarted = LibraryPreferences.ENTRY_NON_STARTED in this,
        skipOutsideReleasePeriod = LibraryPreferences.ENTRY_OUTSIDE_RELEASE_PERIOD in this,
    )
}
