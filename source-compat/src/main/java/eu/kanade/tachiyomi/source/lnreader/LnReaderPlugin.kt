package eu.kanade.tachiyomi.source.lnreader

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import mihon.script.runtime.ScriptRuntime

/** A loaded plugin. Calls fail with a `ScriptException`; a plugin's own error is `ScriptException.Thrown`. */
class LnReaderPlugin internal constructor(private val runtime: ScriptRuntime) {
    /**
     * `id`, `name`, `site`, `version`, `icon`, `filters`, `pluginSettings`, `imageRequestInit` (with LNReader's default
     * User-Agent), `webStorageUtilized`, `customJS`, `customCSS` as the plugin sets them, absent when it doesn't, and
     * `methods`: the names of every method it has, `parsePage` and `resolveUrl` among them when it offers them.
     */
    suspend fun description(): JsonObject = runtime.call("__lnreaderDescribe", JsonArray(emptyList())).jsonObject

    /** Calls the plugin's [method] with [args] and returns its awaited result. */
    suspend fun call(method: String, args: JsonArray): JsonElement =
        runtime.call("__lnreaderInvoke", JsonArray(listOf(JsonPrimitive(method), args)))
}
