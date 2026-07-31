package mihon.entry.interactions.child

import mihon.entry.interactions.child.backup.ENTRY_CHILD_GROUP_FILTER_BACKUP_RESTORE_PARTICIPANT
import mihon.entry.interactions.child.backup.ENTRY_CHILD_GROUP_FILTER_BACKUP_SCHEMA_VERSION
import mihon.entry.interactions.child.backup.ENTRY_CHILD_GROUP_FILTER_BACKUP_SNAPSHOT_PARTICIPANT
import mihon.entry.interactions.child.backup.ENTRY_CHILD_GROUP_FILTER_BACKUP_STATE_ID
import mihon.entry.interactions.child.backup.EntryChildGroupFilterBackupContributor
import mihon.entry.interactions.child.backup.EntryChildGroupFilterBackupState
import mihon.entry.interactions.child.lifecycle.ENTRY_CHILD_GROUP_FILTER_PROFILE_MOVE_PARTICIPANT
import mihon.entry.interactions.child.lifecycle.EntryChildGroupFilterProfileMoveContributor
import mihon.entry.interactions.lifecycle.profile.stateRequest
import mihon.entry.interactions.persistence.backup.decodeEntryBackupState
import mihon.entry.interactions.persistence.backup.entryBackupStateEnvelope
import mihon.entry.interactions.runtime.EntryInteractions
import mihon.entry.interactions.runtime.production.EntryFeatureRuntimeArtifacts
import mihon.entry.interactions.runtime.production.EntryFeatureRuntimeModule
import mihon.entry.interactions.runtime.production.entryFeatureRuntimeBoundary
import mihon.feature.graph.execution.FeatureExecutionHandler
import mihon.feature.graph.execution.FeatureExecutionParticipantBinding
import mihon.feature.runtime.FeatureRuntimeComposition
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

internal val EntryChildListFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.child-list",
    contributor = EntryChildListFeatureContributor,
) {
    addSingletonFactory<EntryChildListFeature> {
        val composition = get<FeatureRuntimeComposition>()
        DefaultEntryChildListFeature(
            evaluation = composition.evaluation,
            childList = get<EntryInteractions>().childList,
            childProgress = get<EntryInteractions>().childProgress,
            missingChildGap = get<EntryInteractions>().missingChildGap,
        )
    }
    EntryFeatureRuntimeArtifacts(
        runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntryChildListFeature>() }),
    )
}

internal val EntryChildGroupFilterFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.child-group-filter",
    contributor = EntryChildGroupFilterFeatureContributor,
    additionalContributors = listOf(
        EntryChildGroupFilterProfileMoveContributor,
        EntryChildGroupFilterBackupContributor,
    ),
) { context ->
    addSingletonFactory<EntryChildGroupFilterFeature> {
        val composition = get<FeatureRuntimeComposition>()
        DefaultEntryChildGroupFilterFeature(
            evaluation = composition.evaluation,
            interaction = get<EntryInteractions>().childGroupFilter,
            dataSource = get(),
        )
    }
    EntryFeatureRuntimeArtifacts(
        executionBindings = listOf(
            FeatureExecutionParticipantBinding(
                definition = ENTRY_CHILD_GROUP_FILTER_BACKUP_SNAPSHOT_PARTICIPANT,
                handler = FeatureExecutionHandler { event ->
                    val result =
                        get<EntryChildGroupFilterFeature>().snapshot(event.profileId, event.entry)
                    if (result is EntryChildGroupFilterSnapshotResult.Available && result.excludedGroups.isNotEmpty()) {
                        event.contributions.add(
                            entryBackupStateEnvelope(
                                ENTRY_CHILD_GROUP_FILTER_BACKUP_STATE_ID,
                                ENTRY_CHILD_GROUP_FILTER_BACKUP_SCHEMA_VERSION,
                                EntryChildGroupFilterBackupState.serializer(),
                                EntryChildGroupFilterBackupState(result.excludedGroups),
                            ),
                        )
                    }
                },
            ),
            FeatureExecutionParticipantBinding(
                definition = ENTRY_CHILD_GROUP_FILTER_BACKUP_RESTORE_PARTICIPANT,
                handler = FeatureExecutionHandler { event ->
                    val state = event.states.decodeEntryBackupState(
                        ENTRY_CHILD_GROUP_FILTER_BACKUP_STATE_ID,
                        ENTRY_CHILD_GROUP_FILTER_BACKUP_SCHEMA_VERSION,
                        EntryChildGroupFilterBackupState.serializer(),
                    ) ?: return@FeatureExecutionHandler
                    get<EntryChildGroupFilterFeature>().restore(event.entry, state.excludedGroups)
                },
            ),
            FeatureExecutionParticipantBinding(
                definition = ENTRY_CHILD_GROUP_FILTER_PROFILE_MOVE_PARTICIPANT,
                handler = FeatureExecutionHandler { event ->
                    context.dependencies.profileMoveChildGroupFilterStateHost.move(event.plan.stateRequest())
                },
            ),
        ),
        runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntryChildGroupFilterFeature>() }),
    )
}

internal val EntryRelatedEntriesFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.related-entries",
    contributor = EntryRelatedEntriesFeatureContributor,
) {
    addSingletonFactory<EntryRelatedEntriesFeature> {
        val composition = get<FeatureRuntimeComposition>()
        DefaultEntryRelatedEntriesFeature(
            evaluation = composition.evaluation,
            sourceManager = get(),
            networkToLocalEntry = get(),
            getEntry = get(),
            sourceDescription = get(),
        )
    }
    EntryFeatureRuntimeArtifacts(
        runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntryRelatedEntriesFeature>() }),
    )
}
