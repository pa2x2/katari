package eu.kanade.domain.source.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

/** Preserve unsupported or malformed filter values verbatim so repair never erases unrelated preset data. */
object FilterStateNodeSerializer : KSerializer<FilterStateNode> {
    override val descriptor = buildClassSerialDescriptor("FilterStateNode")

    override fun deserialize(decoder: Decoder): FilterStateNode {
        val input = decoder as JsonDecoder
        val element = input.decodeJsonElement()
        val raw = element as? JsonObject ?: return FilterStateNode.Unknown(element)
        return runCatching {
            when ((raw["type"] as? JsonPrimitive)?.content) {
                "header" -> input.json.decodeFromJsonElement<FilterStateNode.Header>(raw)
                "separator" -> input.json.decodeFromJsonElement<FilterStateNode.Separator>(raw)
                "select" -> input.json.decodeFromJsonElement<FilterStateNode.Select>(raw)
                "text" -> input.json.decodeFromJsonElement<FilterStateNode.Text>(raw)
                "checkbox" -> input.json.decodeFromJsonElement<FilterStateNode.CheckBox>(raw)
                "tristate" -> input.json.decodeFromJsonElement<FilterStateNode.TriState>(raw)
                "sort" -> input.json.decodeFromJsonElement<FilterStateNode.Sort>(raw)
                "group" -> input.json.decodeFromJsonElement<FilterStateNode.Group>(raw)
                "paged_group" -> input.json.decodeFromJsonElement<FilterStateNode.PagedGroup>(raw)
                else -> FilterStateNode.Unknown(raw)
            }
        }.getOrElse { FilterStateNode.Unknown(raw) }
    }

    override fun serialize(encoder: Encoder, value: FilterStateNode) {
        val output = encoder as JsonEncoder
        if (value is FilterStateNode.Unknown) {
            output.encodeJsonElement(value.raw)
            return
        }
        val (type, fields) = when (value) {
            is FilterStateNode.Header -> "header" to output.json.encodeToJsonElement(value)
            is FilterStateNode.Separator -> "separator" to output.json.encodeToJsonElement(value)
            is FilterStateNode.Select -> "select" to output.json.encodeToJsonElement(value)
            is FilterStateNode.Text -> "text" to output.json.encodeToJsonElement(value)
            is FilterStateNode.CheckBox -> "checkbox" to output.json.encodeToJsonElement(value)
            is FilterStateNode.TriState -> "tristate" to output.json.encodeToJsonElement(value)
            is FilterStateNode.Sort -> "sort" to output.json.encodeToJsonElement(value)
            is FilterStateNode.Group -> "group" to output.json.encodeToJsonElement(value)
            is FilterStateNode.PagedGroup -> "paged_group" to output.json.encodeToJsonElement(value)
            is FilterStateNode.Unknown -> error("Handled above")
        }
        output.encodeJsonElement(JsonObject(fields.jsonObject + ("type" to JsonPrimitive(type))))
    }
}
