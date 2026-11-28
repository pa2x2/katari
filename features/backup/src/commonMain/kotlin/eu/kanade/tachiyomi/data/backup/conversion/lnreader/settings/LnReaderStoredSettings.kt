package eu.kanade.tachiyomi.data.backup.conversion.lnreader.settings

import eu.kanade.tachiyomi.data.backup.conversion.lnreader.LnReaderNovel
import eu.kanade.tachiyomi.data.backup.conversion.lnreader.LnReaderPlugin
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

/**
 * LNReader's `Setting.json`: every key of its settings store with a value that isn't false or empty, settings objects
 * and lists as the JSON text they are stored as, other values as themselves (src/services/backup/utils.ts). Keys hold
 * app state and per-novel data beside settings, so the file is read key by key.
 */
internal class LnReaderStoredSettings(private val values: JsonObject, private val json: Json) {
    val keys: Set<String> get() = values.keys

    /** The settings object or list at [key]; null where the file lacks it, a failure where it can't be read. */
    fun <T> decode(key: String, deserializer: DeserializationStrategy<T>): Result<T>? {
        val text = text(key) ?: return null
        return runCatching { json.decodeFromString(deserializer, text) }
    }

    /** The text at [key]: a setting kept as text, or the JSON text of an object or list. */
    fun text(key: String): String? = (values[key] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull

    /** Whether the switch at [key] is on; LNReader keeps it as a boolean or as its text. */
    fun isOn(key: String): Boolean {
        val value = values[key] as? JsonPrimitive ?: return false
        return value.booleanOrNull ?: (value.contentOrNull == "true")
    }

    /** The plugins' scripts every repository offered, from the cache of their lists. */
    val availablePluginUrls: List<String>
        get() = decode(AVAILABLE_PLUGINS, ListSerializer(LnReaderPlugin.serializer()))?.getOrNull()
            .orEmpty()
            .map { it.url }

    /** LNReader's ids of the novels linked to a tracker. */
    val trackedNovelIds: Set<Long> = keys.mapNotNullTo(hashSetOf()) { key ->
        TRACKED_NOVEL.matchEntire(key)?.groupValues?.get(1)?.toLongOrNull()
    }

    val app: Result<LnReaderAppSettings>? by lazy { decode(APP_SETTINGS, LnReaderAppSettings.serializer()) }

    val browse: Result<LnReaderBrowseSettings>? by lazy { decode(BROWSE_SETTINGS, LnReaderBrowseSettings.serializer()) }

    val library: Result<LnReaderLibrarySettings>? by lazy {
        decode(LIBRARY_SETTINGS, LnReaderLibrarySettings.serializer())
    }

    val chapterGeneral: Result<LnReaderChapterGeneralSettings>? by lazy {
        decode(CHAPTER_GENERAL_SETTINGS, LnReaderChapterGeneralSettings.serializer())
    }

    val chapterReader: Result<LnReaderChapterReaderSettings>? by lazy {
        decode(CHAPTER_READER_SETTINGS, LnReaderChapterReaderSettings.serializer())
    }

    /** The order of a novel's chapters where the novel sets none. */
    val defaultChapterOrder: LnReaderChapterOrder by lazy {
        app?.getOrNull()?.defaultChapterSort?.let(LnReaderChapterOrder::of) ?: LnReaderChapterOrder.default
    }

    fun chapterList(novel: LnReaderNovel): LnReaderChapterList {
        val stored = decode("$NOVEL_SETTINGS_PREFIX${novel.pluginId}_${novel.path}", LnReaderNovelSettings.serializer())
        return LnReaderChapterList(stored?.getOrNull(), defaultChapterOrder)
    }

    companion object {
        const val APP_SETTINGS = "APP_SETTINGS"
        const val BROWSE_SETTINGS = "BROWSE_SETTINGS"
        const val LIBRARY_SETTINGS = "LIBRARY_SETTINGS"
        const val CHAPTER_GENERAL_SETTINGS = "CHAPTER_GENERAL_SETTINGS"
        const val CHAPTER_READER_SETTINGS = "CHAPTER_READER_SETTINGS"
        const val AVAILABLE_PLUGINS = "AVAILABLE_PLUGINS"
        const val NOVEL_SETTINGS_PREFIX = "NOVEL_SETTINGS_"
        const val TRACKED_NOVEL_PREFIX = "TRACKED_NOVEL_PREFIX_"

        // TRACKED_NOVEL_PREFIX_<novelId>_<tracker>, or TRACKED_NOVEL_PREFIX_<novelId> before a tracker per novel
        private val TRACKED_NOVEL = Regex("""${TRACKED_NOVEL_PREFIX}(\d+)(_.+)?""")
    }
}
