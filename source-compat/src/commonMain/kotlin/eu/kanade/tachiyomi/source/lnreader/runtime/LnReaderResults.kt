package eu.kanade.tachiyomi.source.lnreader.runtime

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull

/*
 * Plugins return whatever JavaScript lets them, and LNReader shows what it can of it, so what they return is read
 * leniently: a value of another type than expected reads as absent.
 */

internal fun JsonElement?.asText(): String? = (this as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.contentOrNull

internal fun JsonElement?.asNumber(): Double? = (this as? JsonPrimitive)?.takeUnless(
    JsonPrimitive::isString,
)?.doubleOrNull

internal fun JsonElement?.asBoolean(): Boolean? = (this as? JsonPrimitive)?.takeUnless(
    JsonPrimitive::isString,
)?.booleanOrNull

internal fun JsonElement?.asObject(): JsonObject? = this as? JsonObject

internal fun JsonElement?.asArray(): JsonArray = this as? JsonArray ?: JsonArray(emptyList())

internal fun JsonObject.text(name: String): String? = get(name).asText()
