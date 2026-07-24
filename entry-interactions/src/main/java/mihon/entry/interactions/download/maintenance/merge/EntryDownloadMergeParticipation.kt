package mihon.entry.interactions

import kotlinx.serialization.json.Json
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureDurableExecutionParticipantBinding
import mihon.feature.graph.FeatureDurableExecutionPayload
import mihon.feature.graph.FeatureExecutionParticipantDefinition
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor

internal object EntryDownloadMergeDurableBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.download.maintenance.merge-removal.behavior")
}

internal val ENTRY_DOWNLOAD_MERGE_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.merge.download-removal"),
    owner = ENTRY_DOWNLOAD_MAINTENANCE_FEATURE_OWNER,
    point = ENTRY_MERGE_DURABLE_EXECUTION_POINT,
    prerequisites = CapabilityExpression.Provided(EntryDownloadCapability.definition),
    behavioralContracts = listOf(EntryDownloadMergeDurableBehaviorContract),
)

internal object EntryDownloadMergeContributor : FeatureGraphContributor {
    override val owner = ENTRY_DOWNLOAD_MAINTENANCE_FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(ENTRY_DOWNLOAD_MERGE_PARTICIPANT)
    }
}

internal fun entryDownloadMergeBinding(
    feature: () -> EntryDownloadMaintenanceFeature,
): FeatureDurableExecutionParticipantBinding<EntryMergeDurableEvent> {
    val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }
    return FeatureDurableExecutionParticipantBinding(
        definition = ENTRY_DOWNLOAD_MERGE_PARTICIPANT,
        preparer = { event ->
            val removalRequired = event.downloadRemovalRequested ||
                EntryMergeDurableChange.REMOVED_FROM_LIBRARY in event.changes
            if (!removalRequired) {
                null
            } else {
                when (val preparation = feature().prepareRemoval(event.entry)) {
                    is EntryDownloadRemovalPreparation.Prepared -> FeatureDurableExecutionPayload(
                        schemaVersion = 2,
                        value = json.encodeToString(EntryDownloadRemovalPlan.serializer(), preparation.plan),
                    )
                    EntryDownloadRemovalPreparation.NothingToRemove,
                    is EntryDownloadRemovalPreparation.Inapplicable,
                    -> null
                }
            }
        },
        deliveryHandler = { payload ->
            require(payload.schemaVersion == 2) {
                "Unsupported Download Merge payload ${payload.schemaVersion}"
            }
            val plan = json.decodeFromString(EntryDownloadRemovalPlan.serializer(), payload.value)
            check(feature().applyRemoval(plan) == EntryDownloadMaintenanceResult.Performed) {
                "Merge download removal was not verified"
            }
        },
    )
}
