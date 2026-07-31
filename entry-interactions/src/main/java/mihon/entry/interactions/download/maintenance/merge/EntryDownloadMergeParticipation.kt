package mihon.entry.interactions.download.maintenance.merge

import kotlinx.serialization.json.Json
import mihon.entry.interactions.download.ENTRY_DOWNLOAD_MAINTENANCE_FEATURE_OWNER
import mihon.entry.interactions.download.EntryDownloadCapability
import mihon.entry.interactions.download.EntryDownloadMaintenanceFeature
import mihon.entry.interactions.download.EntryDownloadMaintenanceResult
import mihon.entry.interactions.download.EntryDownloadRemovalPlan
import mihon.entry.interactions.download.EntryDownloadRemovalPreparation
import mihon.entry.interactions.merge.consequence.ENTRY_MERGE_DURABLE_EXECUTION_POINT
import mihon.entry.interactions.merge.consequence.EntryMergeDurableChange
import mihon.entry.interactions.merge.consequence.EntryMergeDurableEvent
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.execution.FeatureDurableExecutionParticipantBinding
import mihon.feature.graph.execution.FeatureDurableExecutionPayload
import mihon.feature.graph.execution.FeatureExecutionParticipantDefinition

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
