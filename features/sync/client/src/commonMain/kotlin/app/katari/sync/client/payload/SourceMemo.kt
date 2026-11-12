package app.katari.sync.client.payload

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/**
 * A source's memo of an entry or chapter: what its extension stored with it and reads back to load it, in whatever
 * shape the extension chose.
 */
typealias SourceMemo =
    @Serializable(with = SourceMemoSerializer::class)
    JsonObject

/** Protobuf has no JSON type, so a memo travels as its JSON text. */
object SourceMemoSerializer : KSerializer<JsonObject> {
    override val descriptor =
        PrimitiveSerialDescriptor("app.katari.sync.client.payload.SourceMemo", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: JsonObject) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): JsonObject = Json.parseToJsonElement(decoder.decodeString()).jsonObject
}
