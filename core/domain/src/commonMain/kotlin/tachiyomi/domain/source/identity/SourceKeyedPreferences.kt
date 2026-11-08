package tachiyomi.domain.source.identity

import eu.kanade.domain.source.service.SourcePreferences
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull
import mihon.core.common.CustomPreferences
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.updates.service.UpdatesPreferences

/**
 * The preferences that hold the keys this device stores sources under, each in the format its owner stores it in,
 * for whatever carries preferences to another device, whose keys differ: backups and sync. The per-source display mode
 * holds the key in its name. App-state preferences, such as the last used source, never leave the device.
 */
object SourceKeyedPreferences {
    /**
     * The preference [key] holding [value], as a store holds it, with every source key in either replaced by what
     * [transform] makes of it; unchanged when it holds none.
     */
    fun map(key: String, value: Any?, transform: (Long) -> Long): Pair<String, Any?> =
        mapKey(key, transform) to mapValue(key, value, transform)

    /** [key] with the source key it names replaced by what [transform] makes of it; unchanged when it names none. */
    fun mapKey(key: String, transform: (Long) -> Long): String {
        val family = SourcePreferences.SOURCE_DISPLAY_MODE_KEY_FAMILY
        val sourceKey = key.removePrefix(family.value).takeIf { key.startsWith(family.value) }?.toLongOrNull()
        return if (sourceKey != null) family.key(transform(sourceKey)) else key
    }

    /** [value] of the preference [key] with every source key in it replaced by what [transform] makes of it. */
    fun mapValue(key: String, value: Any?, transform: (Long) -> Long): Any? {
        return when (value) {
            is Set<*> -> when (key) {
                in KEY_SET_PREFERENCES -> value.mapTo(LinkedHashSet()) { (it as String).mapSourceKey(transform) }
                LibraryPreferences.UPDATE_PAUSED_SOURCES_PREF_KEY ->
                    value.mapTo(LinkedHashSet()) { (it as String).mapLeadingKey(transform) }
                else -> value
            }
            is String -> when (key) {
                in KEY_LIST_PREFERENCES -> value.split(",").joinToString(",") { it.mapSourceKey(transform) }
                CustomPreferences.BROWSE_LONG_PRESS_ACTION_OVERRIDES_KEY ->
                    value.split(";").joinToString(";") { it.mapLeadingKey(transform) }
                in FEED_PREFERENCES -> value.mapFeedSourceKeys(transform)
                else -> value
            }
            else -> value
        }
    }

    private val KEY_SET_PREFERENCES = setOf(
        SourcePreferences.HIDDEN_SOURCES_KEY,
        SourcePreferences.PINNED_SOURCES_KEY,
        // Converted when restored, so they have to arrive with this device's keys
        SourcePreferences.LEGACY_HIDDEN_SOURCES_KEY,
        SourcePreferences.MANGA_HIDDEN_SOURCES_KEY,
        SourcePreferences.ANIME_HIDDEN_SOURCES_KEY,
        SourcePreferences.MANGA_PINNED_SOURCES_KEY,
        SourcePreferences.ANIME_PINNED_SOURCES_KEY,
        LibraryPreferences.LEGACY_UPDATE_EXCLUDED_SOURCES_PREF_KEY,
    )

    private val KEY_LIST_PREFERENCES = setOf(
        SourcePreferences.MIGRATION_SOURCES_KEY,
        UpdatesPreferences.FILTER_INCLUDED_SOURCES_KEY,
        UpdatesPreferences.FILTER_EXCLUDED_SOURCES_KEY,
    )

    private val FEED_PREFERENCES = setOf(
        SourcePreferences.SAVED_FEEDS_KEY,
        SourcePreferences.SAVED_FEED_PRESETS_KEY,
    )

    private const val FEED_SOURCE_KEY_FIELD = "sourceId"

    private fun String.mapSourceKey(transform: (Long) -> Long): String {
        return toLongOrNull()?.let { transform(it).toString() } ?: this
    }

    // "<key>:<rest>"
    private fun String.mapLeadingKey(transform: (Long) -> Long): String {
        val key = substringBefore(':').toLongOrNull() ?: return this
        return "${transform(key)}:${substringAfter(':', "")}"
    }

    private fun String.mapFeedSourceKeys(transform: (Long) -> Long): String {
        val feeds = runCatching { Json.parseToJsonElement(this) }.getOrNull() as? JsonArray ?: return this
        return JsonArray(
            feeds.map { feed ->
                val key = ((feed as? JsonObject)?.get(FEED_SOURCE_KEY_FIELD) as? JsonPrimitive)?.longOrNull
                    ?: return@map feed
                JsonObject(feed + (FEED_SOURCE_KEY_FIELD to JsonPrimitive(transform(key))))
            },
        ).toString()
    }
}
