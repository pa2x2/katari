package eu.kanade.tachiyomi.source.lnreader

import eu.kanade.tachiyomi.source.lnreader.runtime.asArray
import eu.kanade.tachiyomi.source.lnreader.runtime.asBoolean
import eu.kanade.tachiyomi.source.lnreader.runtime.asObject
import eu.kanade.tachiyomi.source.lnreader.runtime.asText
import eu.kanade.tachiyomi.source.lnreader.runtime.text
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * What a plugin says about itself, as far as its source needs it. Reading it takes loading the plugin, seconds on a
 * phone, so the loader keeps it beside the plugin's file and reads it again only for a changed file.
 *
 * @property filters the plugin's filter definitions as it declares them, which searches send back with their values.
 * @property settings the plugin's `pluginSettings` definitions.
 * @property imageHeaders the headers of its `imageRequestInit`, which its covers and pictures load with.
 * @property webStorage whether it reads the site's own web storage (`webStorageUtilized`).
 * @property resolvesUrls whether it has `resolveUrl`, which makes a path a page for WebView.
 * @property pagedChapters whether it has `parsePage`, which reads one page of a chapter list.
 * @property readsLatest whether its code reads `showLatestNovels`; without it, Latest gives the popular list again.
 */
@Serializable
class LnReaderPluginDescription(
    val id: String,
    val name: String,
    val version: String,
    val site: String,
    val filters: JsonObject? = null,
    val settings: JsonObject? = null,
    val imageHeaders: Map<String, String> = emptyMap(),
    val webStorage: Boolean = false,
    val resolvesUrls: Boolean = false,
    val pagedChapters: Boolean = false,
    val readsLatest: Boolean = false,
) {
    internal companion object {
        /** From what the runtime's `__lnreaderDescribe` gives, and whether the plugin's code mentions Latest. */
        fun of(described: JsonObject, readsLatest: Boolean): LnReaderPluginDescription {
            val methods = described["methods"].asArray().mapNotNull { it.asText() }.toSet()
            val headers = described["imageRequestInit"].asObject()?.get("headers").asObject()
            return LnReaderPluginDescription(
                id = checkNotNull(described.text("id")?.takeIf(String::isNotBlank)) { "The plugin has no id" },
                name = described.text("name").orEmpty(),
                version = checkNotNull(described.text("version")?.takeIf(String::isNotBlank)) {
                    "The plugin has no version"
                },
                site = described.text("site").orEmpty(),
                filters = described["filters"].asObject(),
                settings = described["pluginSettings"].asObject(),
                imageHeaders = headers.orEmpty().mapNotNull { (name, value) ->
                    value.asText()?.let { name to it }
                }.toMap(),
                webStorage = described["webStorageUtilized"].asBoolean() == true,
                resolvesUrls = "resolveUrl" in methods,
                pagedChapters = "parsePage" in methods,
                readsLatest = readsLatest,
            )
        }
    }
}
