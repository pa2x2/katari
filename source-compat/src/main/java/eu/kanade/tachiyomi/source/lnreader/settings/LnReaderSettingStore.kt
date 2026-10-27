package eu.kanade.tachiyomi.source.lnreader.settings

import android.content.SharedPreferences
import androidx.preference.PreferenceDataStore
import eu.kanade.tachiyomi.source.lnreader.runtime.asArray
import eu.kanade.tachiyomi.source.lnreader.runtime.asBoolean
import eu.kanade.tachiyomi.source.lnreader.runtime.asObject
import eu.kanade.tachiyomi.source.lnreader.runtime.asText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Keeps a plugin's settings where the plugin reads them through `@libs/storage`: under each setting's key in the
 * plugin's settings file, in LNReader's envelope `{created, value}`, as LNReader's settings screen writes them.
 *
 * Like LNReader, it writes only a changed value, not the default the screen shows before any change. A plugin reads
 * its settings as it's constructed, so each write [reload]s it.
 */
internal class LnReaderSettingStore(
    private val preferences: () -> SharedPreferences,
    settings: List<LnReaderSetting>,
    private val reload: () -> Unit,
) : PreferenceDataStore() {
    private val defaults = settings.associate { it.key to it.default }

    override fun getBoolean(key: String, defValue: Boolean): Boolean = read(key).asBoolean() ?: defValue

    override fun putBoolean(key: String, value: Boolean) = write(key, JsonPrimitive(value))

    override fun getString(key: String, defValue: String?): String? = read(key).asText() ?: defValue

    override fun putString(key: String, value: String?) = write(key, JsonPrimitive(value.orEmpty()))

    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? =
        (read(key) as? JsonArray)?.mapNotNull(JsonElement::asText)?.toSet() ?: defValues

    override fun putStringSet(key: String, values: Set<String>?) =
        write(key, JsonArray(values.orEmpty().map(::JsonPrimitive)))

    private fun read(key: String): JsonElement? = preferences().getString(key, null)
        ?.let { runCatching { Json.parseToJsonElement(it).asObject()?.get("value") }.getOrNull() }

    private fun write(key: String, value: JsonElement) {
        if (same(value, read(key) ?: defaults[key])) return
        val envelope = buildJsonObject {
            put("created", Instant.now().truncatedTo(ChronoUnit.MILLIS).toString())
            put("value", value)
        }
        preferences().edit().putString(key, envelope.toString()).apply()
        reload()
    }

    // A checkbox group's choices are a set, whatever order they were stored in
    private fun same(value: JsonElement, current: JsonElement?): Boolean = when (value) {
        is JsonArray -> current is JsonArray && value.toSet() == current.asArray().toSet()
        else -> value == current
    }
}
