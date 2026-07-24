package mihon.entry.interactions

import mihon.feature.graph.FeatureExecutionHandler
import mihon.feature.graph.FeatureExecutionParticipantBinding
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

internal val EntryChildListFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.child-list",
    contributor = EntryChildListFeatureContributor,
) {
    addSingletonFactory<EntryChildListFeature> {
        val composition = get<EntryInteractionComposition>()
        DefaultEntryChildListFeature(
            evaluation = composition.featureGraphEvaluation,
            childList = composition.interactions.childList,
            childProgress = composition.interactions.childProgress,
            missingChildGap = composition.interactions.missingChildGap,
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
        val composition = get<EntryInteractionComposition>()
        DefaultEntryChildGroupFilterFeature(
            evaluation = composition.featureGraphEvaluation,
            interaction = composition.interactions.childGroupFilter,
            dataSource = get(),
        )
    }
    EntryFeatureRuntimeArtifacts(
        executionBindings = listOf(
            FeatureExecutionParticipantBinding(
                definition = ENTRY_CHILD_GROUP_FILTER_BACKUP_SNAPSHOT_PARTICIPANT,
                handler = FeatureExecutionHandler { event ->
                    val result = get<EntryChildGroupFilterFeature>().snapshot(event.profileId, event.entry)
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
            mihon.feature.graph.FeatureExecutionParticipantBinding(
                definition = ENTRY_CHILD_GROUP_FILTER_PROFILE_MOVE_PARTICIPANT,
                handler = mihon.feature.graph.FeatureExecutionHandler { event ->
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
        val composition = get<EntryInteractionComposition>()
        DefaultEntryRelatedEntriesFeature(
            evaluation = composition.featureGraphEvaluation,
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
