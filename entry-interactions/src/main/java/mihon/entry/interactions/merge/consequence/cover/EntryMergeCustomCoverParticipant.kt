package mihon.entry.interactions.merge.consequence.cover

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
import mihon.entry.interactions.merge.host.EntryMergeHost
import tachiyomi.domain.entry.model.Entry

internal const val ENTRY_MERGE_CUSTOM_COVER_PARTICIPANT_ID = "entry.merge.cover-cleanup"

internal class EntryMergeCustomCoverParticipant(
    private val mergeHost: EntryMergeHost,
    private val cleanup: suspend (Entry) -> Unit,
) : EntryDurableParticipant<EntryMergeDurableEvent> {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    override val id = ENTRY_MERGE_CUSTOM_COVER_PARTICIPANT_ID

    override suspend fun prepare(event: EntryMergeDurableEvent): EntryDurablePayload? {
        if (EntryMergeDurableChange.REMOVED_FROM_LIBRARY !in event.changes) return null
        return EntryDurablePayload(
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

    override suspend fun deliver(payload: EntryDurablePayload) {
        require(payload.schemaVersion == 2) { "Unsupported custom-cover Merge payload ${payload.schemaVersion}" }
        val subject = json.parseToJsonElement(payload.value).jsonObject
        val profileId = subject.getValue("profileId").jsonPrimitive.long
        val entryId = subject.getValue("entryId").jsonPrimitive.long
        mergeHost.profile(profileId).entries(listOf(entryId)).singleOrNull()?.let { cleanup(it) }
    }
}
