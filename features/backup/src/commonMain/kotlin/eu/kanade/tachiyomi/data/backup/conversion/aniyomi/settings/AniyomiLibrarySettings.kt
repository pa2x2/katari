package eu.kanade.tachiyomi.data.backup.conversion.aniyomi.settings

import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Item
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Name
import eu.kanade.tachiyomi.data.backup.conversion.aniyomi.hidesPreviews
import eu.kanade.tachiyomi.data.backup.conversion.aniyomi.katariFillerFilter
import eu.kanade.tachiyomi.data.backup.conversion.aniyomi.katariSorting
import eu.kanade.tachiyomi.data.backup.models.BooleanPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.IntPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.LongPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.PreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringSetPreferenceValue
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.*

/**
 * Aniyomi's anime library, episode list, download and category settings, which Katari keeps once for every kind:
 * one value wins (D062), category sets are joined (D063).
 */
internal fun AniyomiSettings.convertLibrarySettings(animeCategoryId: (Long) -> Long?) {
    convertLibrarySort()
    SAME_MEANING.forEach { (mangaKey, animeKey) -> preferAnime(mangaKey, animeKey) { it } }
    convertEpisodeListDefaults()
    convertSwipe("pref_chapter_swipe_start_action", "pref_episode_swipe_start_action")
    convertSwipe("pref_chapter_swipe_end_action", "pref_episode_swipe_end_action")
    preferAnime(LibraryPreferences.DEFAULT_CATEGORY_PREF_KEY, "default_anime_category") { value ->
        val id = (value as? IntPreferenceValue)?.value ?: return@preferAnime null
        // Below 1 is no category of the list: always ask, or the default one
        if (id < 1) value else animeCategoryId(id.toLong())?.let { IntPreferenceValue(it.toInt()) }
    }
    CATEGORY_SETS.forEach { (mangaKey, animeKey) ->
        joinSets(mangaKey, animeKey) { id -> id.toLongOrNull()?.let(animeCategoryId)?.toString() }
    }
    joinSets("mark_duplicate_read_chapter_read", "mark_duplicate_seen_episode_seen") { DUPLICATE_EPISODE_RULES[it] }
    take("library_random_manga_sort_seed")?.let { writeAsMihon("library_random_sort_seed", it) }
}

/**
 * The manga set at [mangaKey] joined with the anime set at [animeKey], each of whose items [fromAnime] gives as Mihon
 * keeps them, as Mihon's setting at [mihonKey].
 */
internal fun AniyomiSettings.joinSets(
    mangaKey: String,
    animeKey: String,
    mihonKey: String = mangaKey,
    fromAnime: (String) -> String?,
) {
    val anime = (take(animeKey) as? StringSetPreferenceValue)?.value
    val manga = (take(mangaKey) as? StringSetPreferenceValue)?.value
    if (anime == null && manga == null) return
    writeAsMihon(mihonKey, StringSetPreferenceValue(manga.orEmpty() + anime.orEmpty().mapNotNull(fromAnime)))
}

// Katari sorts by neither tracker scores nor airing times
private fun AniyomiSettings.convertLibrarySort() {
    preferAnime("library_sorting_mode", "animelib_sorting_mode") { value ->
        val (type, direction) = (value as? StringPreferenceValue)?.value?.split(",")?.takeIf { it.size == 2 }
            ?: return@preferAnime null
        val katariType = LIBRARY_SORT_TYPES[type]
        if (katariType == null) {
            UNSORTABLE[type]?.let { leaveBehind(Item(Name.Setting("library_sorting_mode"), it)) }
            null
        } else {
            StringPreferenceValue("$katariType,$direction")
        }
    }
}

/**
 * The default episode list settings: the seen, downloaded and bookmarked filters keep Aniyomi's bits, the sort moves
 * and the filler filter and previews are Katari's own settings, which Mihon has none of.
 */
private fun AniyomiSettings.convertEpisodeListDefaults() {
    listOf(
        "default_chapter_filter_by_read" to "default_episode_filter_by_seen",
        "default_chapter_filter_by_downloaded" to "default_episode_filter_by_downloaded",
        "default_chapter_filter_by_bookmarked" to "default_episode_filter_by_bookmarked",
    ).forEach { (mangaKey, animeKey) -> preferAnime(mangaKey, animeKey) { it } }
    preferAnime("default_chapter_sort_by_source_or_number", "default_episode_sort_by_source_or_number") { value ->
        (value as? LongPreferenceValue)?.let { LongPreferenceValue(katariSorting(it.value)) }
    }
    (take("default_episode_filter_by_fillermarked") as? LongPreferenceValue)?.let {
        write("default_chapter_filter_by_filler", LongPreferenceValue(katariFillerFilter(it.value)))
    }
    (take("default_episode_show_thumbnail_previews") as? LongPreferenceValue)?.let {
        write("default_chapter_show_previews", BooleanPreferenceValue(!hidesPreviews(it.value)))
    }
}

/** Katari's swipes do nothing where Aniyomi's toggled the filler mark, which Katari has no action for (D073). */
private fun AniyomiSettings.convertSwipe(mangaKey: String, animeKey: String) {
    preferAnime(mangaKey, animeKey) { value ->
        when (val action = (value as? StringPreferenceValue)?.value) {
            "ToggleSeen" -> StringPreferenceValue(LibraryPreferences.ChapterSwipeAction.ToggleRead.name)
            "ToggleFillermark" -> {
                writeUnlisted(mangaKey, StringPreferenceValue(LibraryPreferences.ChapterSwipeAction.Disabled.name))
                leaveBehind(Item(Name.Setting(mangaKey), MR.strings.backup_conversion_aniyomi_filler_action))
                null
            }
            else -> LibraryPreferences.ChapterSwipeAction.entries.find { it.name == action }?.let { value }
        }
    }
}

/** Settings Aniyomi keeps for each kind under the same meaning and type, by Mihon's key and Aniyomi's anime key. */
private val SAME_MEANING = listOf(
    "pref_library_columns_portrait_key" to "pref_animelib_columns_portrait_key",
    "pref_filter_library_downloaded_v2" to "pref_filter_animelib_downloaded_v2",
    "pref_filter_library_unread_v2" to "pref_filter_animelib_unread_v2",
    // Mihon's conversion turns started into Katari's not started
    "pref_filter_library_started_v2" to "pref_filter_animelib_started_v2",
    "pref_filter_library_bookmarked_v2" to "pref_filter_animelib_bookmarked_v2",
    "pref_filter_library_completed_v2" to "pref_filter_animelib_completed_v2",
    "auto_download_while_reading" to "auto_download_while_watching",
    "download_new" to "download_new_episode",
    "download_new_unread_chapters_only" to "download_new_unread_episodes_only",
    "browse_hide_in_library_items" to "browse_hide_in_anime_library_items",
)

/** Category id sets, by Mihon's key and Aniyomi's anime key. */
private val CATEGORY_SETS = listOf(
    LibraryPreferences.LEGACY_UPDATE_CATEGORIES_PREF_KEY to "animelib_update_categories",
    LibraryPreferences.LEGACY_UPDATE_CATEGORIES_EXCLUDE_PREF_KEY to "animelib_update_categories_exclude",
    "remove_exclude_categories" to "remove_exclude_anime_categories",
    "download_new_categories" to "download_new_anime_categories",
    "download_new_categories_exclude" to "download_new_anime_categories_exclude",
)

private val DUPLICATE_EPISODE_RULES = mapOf(
    "new_episode" to LibraryPreferences.MARK_DUPLICATE_CHAPTER_READ_NEW,
    "existing_episode" to LibraryPreferences.MARK_DUPLICATE_CHAPTER_READ_EXISTING,
)

// Aniyomi's AnimeLibrarySort type names and Mihon's
private val LIBRARY_SORT_TYPES = mapOf(
    "ALPHABETICAL" to "ALPHABETICAL",
    "LAST_SEEN" to "LAST_READ",
    "LAST_ANIME_UPDATE" to "LAST_MANGA_UPDATE",
    "UNSEEN_COUNT" to "UNREAD_COUNT",
    "TOTAL_EPISODES" to "TOTAL_CHAPTERS",
    "LATEST_EPISODE" to "LATEST_CHAPTER",
    "EPISODE_FETCH_DATE" to "CHAPTER_FETCH_DATE",
    "DATE_ADDED" to "DATE_ADDED",
    "RANDOM" to "RANDOM",
)

private val UNSORTABLE = mapOf(
    "TRACKER_MEAN" to MR.strings.backup_conversion_aniyomi_sort_tracker_score,
    "AIRING_TIME" to MR.strings.backup_conversion_aniyomi_sort_airing_time,
)
