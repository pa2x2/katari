package eu.kanade.tachiyomi.source.lnreader.runtime

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
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

    /**
     * The plugin's `resolveUrl` of each of [paths] in one call, as a chapter list needs them all; null where it threw
     * or gave no text. Only for a plugin whose description lists `resolveUrl`.
     */
    suspend fun resolveUrls(paths: List<String>, isNovel: Boolean): List<String?> =
        runtime.call(
            "__lnreaderResolveUrls",
            JsonArray(listOf(JsonArray(paths.map(::JsonPrimitive)), JsonPrimitive(isNovel))),
        ).jsonArray.map { (it as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.content }
}
