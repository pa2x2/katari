package mihon.entry.interactions.state

import mihon.entry.interactions.media.session.EntryMediaSessionConsequence
import mihon.entry.interactions.media.session.EntryMediaSessionEvent
import mihon.entry.interactions.persistence.backup.decodeEntryBackupState
import mihon.entry.interactions.persistence.backup.entryBackupStateEnvelope
import mihon.entry.interactions.runtime.EntryInteractions
import mihon.entry.interactions.runtime.production.EntryFeatureRuntimeArtifacts
import mihon.entry.interactions.runtime.production.EntryFeatureRuntimeModule
import mihon.entry.interactions.runtime.production.entryFeatureRuntimeBoundary
import mihon.entry.interactions.state.backup.ENTRY_PROGRESS_BACKUP_RESTORE_PARTICIPANT
import mihon.entry.interactions.state.backup.ENTRY_PROGRESS_BACKUP_SCHEMA_VERSION
import mihon.entry.interactions.state.backup.ENTRY_PROGRESS_BACKUP_SNAPSHOT_PARTICIPANT
import mihon.entry.interactions.state.backup.ENTRY_PROGRESS_BACKUP_STATE_ID
import mihon.entry.interactions.state.backup.EntryProgressBackupContributor
import mihon.entry.interactions.state.migration.EntryProgressMigrationContributor
import mihon.entry.interactions.state.migration.entryProgressMigrationBinding
import mihon.feature.graph.contextEvidence
import mihon.feature.graph.execution.FeatureExecutionContextResolver
import mihon.feature.graph.execution.FeatureExecutionHandler
import mihon.feature.graph.execution.FeatureExecutionParticipantBinding
import mihon.feature.runtime.FeatureRuntimeComposition
import tachiyomi.domain.library.service.LibraryPreferences
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

internal val EntryConsumptionFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.consumption",
    contributor = EntryConsumptionFeatureContributor,
) {
    addSingletonFactory<EntryConsumptionFeature> {
        val composition = get<FeatureRuntimeComposition>()
        DefaultEntryConsumptionFeature(
            evaluation = composition.evaluation,
            interaction = get<EntryInteractions>().consumption,
            downloadLifecycle = get(),
            history = get(),
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
        val composition = get<FeatureRuntimeComposition>()
        DefaultEntryBookmarkFeature(
            evaluation = composition.evaluation,
            interaction = get<EntryInteractions>().bookmark,
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
        val composition = get<FeatureRuntimeComposition>()
        val preferences = get<LibraryPreferences>()
        DefaultEntryUpdateEligibilityFeature(
            evaluation = composition.evaluation,
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
        val composition = get<FeatureRuntimeComposition>()
        DefaultEntryProgressFeature(
            evaluation = composition.evaluation,
            interaction = get<EntryInteractions>().progress,
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
