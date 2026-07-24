package mihon.entry.interactions

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import mihon.entry.interactions.host.EntryMergeHost
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureDurableExecutionParticipantBinding
import mihon.feature.graph.FeatureDurableExecutionPayload
import mihon.feature.graph.FeatureExecutionParticipantDefinition
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import tachiyomi.domain.entry.model.Entry

internal object EntryMergeCustomCoverDurableBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.custom-cover.merge-cleanup.behavior")
}

internal val ENTRY_MERGE_CUSTOM_COVER_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.merge.cover-cleanup"),
    owner = ENTRY_LIBRARY_CUSTOM_COVER_OWNER,
    point = ENTRY_MERGE_DURABLE_EXECUTION_POINT,
    behavioralContracts = listOf(EntryMergeCustomCoverDurableBehaviorContract),
)

internal object EntryMergeCustomCoverContributor : FeatureGraphContributor {
    override val owner = ENTRY_LIBRARY_CUSTOM_COVER_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(ENTRY_MERGE_CUSTOM_COVER_PARTICIPANT)
    }
}

internal fun entryMergeCustomCoverBinding(
    mergeHost: EntryMergeHost,
    cleanup: suspend (Entry) -> Unit,
): FeatureDurableExecutionParticipantBinding<EntryMergeDurableEvent> {
    val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }
    return FeatureDurableExecutionParticipantBinding(
        definition = ENTRY_MERGE_CUSTOM_COVER_PARTICIPANT,
        preparer = { event ->
            if (EntryMergeDurableChange.REMOVED_FROM_LIBRARY !in event.changes) {
                null
            } else {
                FeatureDurableExecutionPayload(
                    schemaVersion = 2,
                    value = json.encodeToString(
                        JsonObject.serializer(),
                        buildJsonObject {
                            put("profileId", event.entry.profileId)
                            put("entryId", event.entry.id)
                        },
                    ),
                )
            }
        },
        deliveryHandler = { payload ->
            require(payload.schemaVersion == 2) {
                "Unsupported custom-cover Merge payload ${payload.schemaVersion}"
            }
            val subject = json.parseToJsonElement(payload.value).jsonObject
            val profileId = subject.getValue("profileId").jsonPrimitive.long
            val entryId = subject.getValue("entryId").jsonPrimitive.long
            mergeHost.profile(profileId).entries(listOf(entryId)).singleOrNull()?.let {
                cleanup(it)
            }
        },
    )
}
