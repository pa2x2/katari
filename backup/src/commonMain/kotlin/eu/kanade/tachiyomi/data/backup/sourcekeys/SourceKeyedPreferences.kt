package eu.kanade.tachiyomi.data.backup.sourcekeys

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BackupSourcePreferences
import eu.kanade.tachiyomi.data.backup.models.StringPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringSetPreferenceValue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull
import mihon.core.common.CustomPreferences
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.updates.service.UpdatesPreferences

/**
 * The backed-up preferences that hold source keys, each in the format its owner stores it in. App-state preferences,
 * such as the last used source, aren't backed up.
 */
internal fun BackupPreference.mapSourceKeys(transform: (Long) -> Long): BackupPreference {
    SourcePreferences.SOURCE_DISPLAY_MODE_KEY_FAMILY.value.let { prefix ->
        val key = key.removePrefix(prefix).takeIf { key.startsWith(prefix) }?.toLongOrNull()
        if (key != null) return copy(key = SourcePreferences.SOURCE_DISPLAY_MODE_KEY_FAMILY.key(transform(key)))
    }
    val mapped = when (val value = value) {
        is StringSetPreferenceValue -> when (key) {
            in KEY_SET_PREFERENCES -> StringSetPreferenceValue(
                value.value.mapTo(LinkedHashSet()) {
                    it.mapKey(transform)
                },
            )
            LibraryPreferences.UPDATE_PAUSED_SOURCES_PREF_KEY ->
                StringSetPreferenceValue(value.value.mapTo(LinkedHashSet()) { it.mapLeadingKey(transform) })
            else -> null
        }
        is StringPreferenceValue -> when (key) {
            in KEY_LIST_PREFERENCES ->
                StringPreferenceValue(value.value.split(",").joinToString(",") { it.mapKey(transform) })
            CustomPreferences.BROWSE_LONG_PRESS_ACTION_OVERRIDES_KEY ->
                StringPreferenceValue(value.value.split(";").joinToString(";") { it.mapLeadingKey(transform) })
            in FEED_PREFERENCES -> StringPreferenceValue(value.value.mapFeedSourceKeys(transform))
            else -> null
        }
        else -> null
    }
    return mapped?.let { copy(value = it) } ?: this
}

/** Backups from before the key was written separately only have it in the preference file's name. */
internal val BackupSourcePreferences.backupSourceKey: Long?
    get() = sourceId ?: sourceKey.removePrefix("source_").takeIf { sourceKey.startsWith("source_") }?.toLongOrNull()

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

private fun String.mapKey(transform: (Long) -> Long): String {
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
