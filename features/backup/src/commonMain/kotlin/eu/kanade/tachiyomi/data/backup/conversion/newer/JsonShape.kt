package eu.kanade.tachiyomi.data.backup.conversion.newer

import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/**
 * What a JSON file of another app's backup holds, as far as Katari knows it: the keys of the model it is read as
 * ([readAs]), nested objects and lists of them included, and the keys of each object Katari knows and doesn't read
 * ([ignored], by the object's model). [check] records the keys a newer app added (D123); a value of another type than
 * Katari reads fails decoding instead, which leaves its entry behind (D124).
 */
internal class JsonShape(
    private val readAs: SerialDescriptor,
    ignored: Map<SerialDescriptor, Set<String>> = emptyMap(),
) {
    // By name, as a nullable field's descriptor is a different object from its class's
    private val ignored = ignored.mapKeys { it.key.serialName }

    fun check(element: JsonElement, where: List<String>, reading: NewerAppReading) {
        check(readAs, element, where, reading)
    }

    private fun check(
        descriptor: SerialDescriptor,
        element: JsonElement,
        where: List<String>,
        reading: NewerAppReading,
    ) {
        when {
            descriptor.kind == StructureKind.LIST && element is JsonArray ->
                element.forEach { check(descriptor.getElementDescriptor(0), it, where, reading) }
            descriptor.kind == StructureKind.CLASS && element is JsonObject -> {
                val names = descriptor.elementNames.toList()
                val ignoredHere = ignored[descriptor.serialName.removeSuffix("?")].orEmpty()
                element.forEach { (key, value) ->
                    val index = names.indexOf(key)
                    when {
                        index >= 0 -> {
                            val inner = descriptor.getElementDescriptor(index)
                            if (inner.kind == StructureKind.CLASS || inner.kind == StructureKind.LIST) {
                                check(inner, value, where + key, reading)
                            }
                        }
                        key !in ignoredHere -> reading.unknown(where, key, kindOf(value))
                    }
                }
            }
        }
    }

    // How the issue names a value's JSON type
    private fun kindOf(value: JsonElement): String = when (value) {
        is JsonObject -> "JSON object"
        is JsonArray -> "JSON array"
        JsonNull -> "JSON null"
        is JsonPrimitive -> when {
            value.isString -> "JSON string"
            value.booleanOrNull != null -> "JSON boolean"
            else -> "JSON number"
        }
    }
}
