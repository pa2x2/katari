package mihon.entry.interactions.tracking.merge

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import mihon.entry.interactions.durable.EntryDurableParticipant
import mihon.entry.interactions.durable.EntryDurablePayload
import mihon.entry.interactions.merge.consequence.EntryMergeDurableChange
import mihon.entry.interactions.merge.consequence.EntryMergeDurableEvent
import mihon.entry.interactions.tracking.EntryTrackingFeature
import tachiyomi.domain.entry.model.Entry

internal const val ENTRY_TRACKING_MERGE_PARTICIPANT_ID = "entry.merge.library-initialization"

internal class EntryTrackingMergeParticipant(
    private val resolveEntry: suspend (profileId: Long, type: EntryType, sourceId: Long, url: String) -> Entry?,
    private val feature: () -> EntryTrackingFeature,
) : EntryDurableParticipant<EntryMergeDurableEvent> {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    override val id = ENTRY_TRACKING_MERGE_PARTICIPANT_ID

    override suspend fun prepare(event: EntryMergeDurableEvent): EntryDurablePayload? {
        if (EntryMergeDurableChange.ADDED_TO_LIBRARY !in event.changes) return null
        return EntryDurablePayload(
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

    override suspend fun deliver(payload: EntryDurablePayload) {
        require(payload.schemaVersion == 2) { "Unsupported Tracking Merge payload ${payload.schemaVersion}" }
        val identity = json.parseToJsonElement(payload.value).jsonObject
        val profileId = identity.getValue("profileId").jsonPrimitive.long
        val entry = resolveEntry(
            profileId,
            EntryType.valueOf(identity.getValue("type").jsonPrimitive.content),
            identity.getValue("sourceId").jsonPrimitive.long,
            identity.getValue("url").jsonPrimitive.content,
        ) ?: error("Merge-initialized Entry no longer exists")
        feature().bindAutomatically(entry)
    }
}
