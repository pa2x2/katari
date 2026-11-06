package app.katari.sync.client.record

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import kotlin.jvm.JvmInline

/**
 * The values that locate a synced row, as a JSON array in the order its kind declares, the way the capture triggers
 * write it with SQLite's `json_array`. It is compared as text, so one built here holds only integers and plain
 * strings, which both write the same way.
 */
@JvmInline
value class LocalRef(val json: String) {
    private val values: JsonArray get() = Json.parseToJsonElement(json).jsonArray

    fun long(index: Int): Long = values[index].jsonPrimitive.long

    fun longOrNull(index: Int): Long? = values[index].jsonPrimitive.longOrNull

    fun text(index: Int): String = values[index].jsonPrimitive.content

    companion object {
        fun of(vararg values: Any?): LocalRef = LocalRef(
            JsonArray(
                values.map { value ->
                    when (value) {
                        null -> JsonNull
                        is Long -> JsonPrimitive(value)
                        is String -> JsonPrimitive(value)
                        else -> throw IllegalArgumentException("A local ref holds integers and strings only")
                    }
                },
            ).toString(),
        )
    }
}
