package eu.kanade.tachiyomi.data.backup.conversion.mihon

import eu.kanade.domain.source.model.BUILTIN_LATEST_PRESET_ID
import eu.kanade.domain.source.model.FeedListingMode
import eu.kanade.domain.source.model.FilterStateNode
import eu.kanade.domain.source.model.SourceFeed
import eu.kanade.domain.source.model.SourceFeedPreset
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Item
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.StringPreferenceValue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import tachiyomi.i18n.*

/**
 * TachiyomiSY's, Komikku's and Anikku's saved searches and feeds as Katari's, which Katari keeps as two settings (D045,
 * D046, D069).
 */
internal class ConvertedSavedSearches(val preferences: List<BackupPreference>, val carried: List<Item>)

/**
 * [sourceNames] and the searches and feeds name sources by the backup's ids; [sourceKey] gives the key the converted
 * backup lists a source under.
 */
internal fun convertSavedSearches(
    searches: List<MihonSavedSearch>,
    feeds: List<MihonFeed>,
    sourceNames: Map<Long, String>,
    json: Json,
    sourceKey: (Long) -> Long = { it },
): ConvertedSavedSearches {
    // Komikku's feeds hold their saved search, which may be none of the backup's saved searches
    val allSearches = (searches + feeds.mapNotNull { it.savedSearch })
        .distinctBy { Triple(it.source, it.name, it.query) }
    val presets = allSearches.mapIndexed { index, search ->
        search to search.toPreset("$CONVERTED_PRESET_PREFIX$index", sourceKey(search.source), json)
    }
    val presetIds = presets.associate { (search, preset) -> (search.source to search.name) to preset.preset.id }
    val katariFeeds = feeds
        .sortedByDescending { it.global }
        .mapIndexed { index, feed ->
            val searchName = feed.savedSearch?.name ?: feed.savedSearchName
            SourceFeed(
                id = "$CONVERTED_FEED_PREFIX$index",
                sourceId = sourceKey(feed.source),
                // Without a saved search, a feed shows the source's latest titles
                presetId = searchName?.let { presetIds[feed.source to it] } ?: BUILTIN_LATEST_PRESET_ID,
            )
        }

    val preferences = buildList {
        if (presets.isNotEmpty()) {
            val value = json.encodeToString(presets.map { (_, preset) -> preset.preset })
            add(BackupPreference(SourcePreferences.SAVED_FEED_PRESETS_KEY, StringPreferenceValue(value)))
        }
        if (katariFeeds.isNotEmpty()) {
            val value = json.encodeToString(katariFeeds)
            add(BackupPreference(SourcePreferences.SAVED_FEEDS_KEY, StringPreferenceValue(value)))
        }
    }
    val carried = presets.map { (search, preset) ->
        Item(search.name, MR.strings.backup_conversion_filters_partly.takeIf { preset.filtersPartly })
    } + feeds.map { feed ->
        val name = feed.savedSearch?.name ?: feed.savedSearchName ?: sourceNames[feed.source] ?: feed.source.toString()
        Item(name, MR.strings.backup_conversion_feed)
    }
    return ConvertedSavedSearches(preferences, carried)
}

private class ConvertedPreset(val preset: SourceFeedPreset, val filtersPartly: Boolean)

private fun MihonSavedSearch.toPreset(id: String, sourceId: Long, json: Json): ConvertedPreset {
    var partly = false
    val filters = runCatching { json.parseToJsonElement(filterList) }.getOrNull()
        ?.let { (it as? JsonArray)?.toFilterStates { partly = true } }
        ?: emptyList<FilterStateNode>().also { partly = filterList.isNotBlank() }
    return ConvertedPreset(
        SourceFeedPreset(
            id = id,
            sourceId = sourceId,
            name = name,
            listingMode = FeedListingMode.Search,
            query = query.ifBlank { null },
            filters = filters,
        ),
        partly,
    )
}

/**
 * TachiyomiSY's filter JSON (FilterSerializer: `name`, `state` and `_type` per filter, with a group's filters as its
 * state) as Katari's filter states, which name each filter the same way. [onUntranslated] hears of a filter of a kind
 * Katari doesn't keep, which is left out.
 */
private fun JsonArray.toFilterStates(onUntranslated: () -> Unit): List<FilterStateNode> = mapNotNull { element ->
    val filter = element as? JsonObject ?: return@mapNotNull null.also { onUntranslated() }
    val name = filter.string("name") ?: ""
    val state = filter["state"]
    when (filter.string("_type")) {
        "HEADER" -> FilterStateNode.Header(name)
        "SEPARATOR" -> FilterStateNode.Separator(name)
        "CHECKBOX" -> state.primitive()?.let { it.booleanOrNull ?: it.content.toBooleanStrictOrNull() }
            ?.let { FilterStateNode.CheckBox(name, it) }
        "TRISTATE" -> state.int()?.let { FilterStateNode.TriState(name, it) }
        "SELECT" -> state.int()?.let { FilterStateNode.Select(name, it) }
        "TEXT" -> state.primitive()?.let { FilterStateNode.Text(name, it.content) }
        "SORT" -> {
            val selection = state as? JsonObject
            FilterStateNode.Sort(
                name,
                selection?.get("index").int(),
                selection?.get("ascending").primitive()?.booleanOrNull,
            )
        }
        "GROUP" -> (state as? JsonArray)?.let { FilterStateNode.Group(name, it.toFilterStates(onUntranslated)) }
        else -> null
    } ?: null.also { onUntranslated() }
}

private fun JsonObject.string(key: String): String? = get(key).primitive()?.content

private fun JsonElement?.primitive(): JsonPrimitive? = this as? JsonPrimitive

// SY writes numbers as strings
private fun JsonElement?.int(): Int? = primitive()?.let { it.intOrNull ?: it.content.toIntOrNull() }

private const val CONVERTED_PRESET_PREFIX = "converted-search-"
private const val CONVERTED_FEED_PREFIX = "converted-feed-"
