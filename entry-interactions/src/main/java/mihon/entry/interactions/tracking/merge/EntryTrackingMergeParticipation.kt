package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureDurableExecutionParticipantBinding
import mihon.feature.graph.FeatureDurableExecutionPayload
import mihon.feature.graph.FeatureExecutionParticipantDefinition
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import tachiyomi.domain.entry.model.Entry

internal object EntryTrackingMergeDurableBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.tracking.merge-initialization.behavior")
}

internal val ENTRY_TRACKING_MERGE_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.merge.library-initialization"),
    owner = ENTRY_TRACKING_OWNER,
    point = ENTRY_MERGE_DURABLE_EXECUTION_POINT,
    behavioralContracts = listOf(EntryTrackingMergeDurableBehaviorContract),
)

internal object EntryTrackingMergeContributor : FeatureGraphContributor {
    override val owner = ENTRY_TRACKING_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(ENTRY_TRACKING_MERGE_PARTICIPANT)
    }
}

internal fun entryTrackingMergeBinding(
    resolveEntry: suspend (profileId: Long, type: EntryType, sourceId: Long, url: String) -> Entry?,
    feature: () -> EntryTrackingFeature,
): FeatureDurableExecutionParticipantBinding<EntryMergeDurableEvent> {
    val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }
    return FeatureDurableExecutionParticipantBinding(
        definition = ENTRY_TRACKING_MERGE_PARTICIPANT,
        preparer = { event ->
            if (EntryMergeDurableChange.ADDED_TO_LIBRARY !in event.changes) {
                null
            } else {
                FeatureDurableExecutionPayload(
                    schemaVersion = 2,
                    value = json.encodeToString(
                        JsonObject.serializer(),
                        buildJsonObject {
                            put("profileId", event.entry.profileId)
                            put("type", event.entry.type.name)
                            put("sourceId", event.entry.source)
                            put("url", event.entry.url)
                        },
                    ),
                )
            }
        },
        deliveryHandler = { payload ->
            require(payload.schemaVersion == 2) {
                "Unsupported Tracking Merge payload ${payload.schemaVersion}"
            }
            val identity = json.parseToJsonElement(payload.value).jsonObject
            val profileId = identity.getValue("profileId").jsonPrimitive.long
            val entry = resolveEntry(
                profileId,
                EntryType.valueOf(identity.getValue("type").jsonPrimitive.content),
                identity.getValue("sourceId").jsonPrimitive.long,
                identity.getValue("url").jsonPrimitive.content,
            ) ?: error("Merge-initialized Entry no longer exists")
            feature().bindAutomatically(entry)
        },
    )
}
