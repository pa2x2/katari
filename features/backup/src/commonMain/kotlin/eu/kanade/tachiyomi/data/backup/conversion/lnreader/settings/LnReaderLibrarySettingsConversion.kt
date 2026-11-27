package eu.kanade.tachiyomi.data.backup.conversion.lnreader.settings

import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Item
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Name
import eu.kanade.tachiyomi.data.backup.models.BooleanPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.IntPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringSetPreferenceValue
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.library.model.CoverSize
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.*

/**
 * LNReader's library settings: the category for new novels, which categories update, and the library's sort, filter
 * and look.
 *
 * @param categoryIds the ids of the categories the converted backup holds, which restoring finds again by name.
 */
internal fun LnReaderSettings.convertLibrarySettings(settings: LnReaderLibrarySettings, categoryIds: Set<Long>) {
    convertDefaultCategory(settings, categoryIds)
    listOf(
        LibraryPreferences.LEGACY_UPDATE_CATEGORIES_EXCLUDE_PREF_KEY to settings.globalUpdateExcludeCategoryIds,
        LibraryPreferences.LEGACY_UPDATE_CATEGORIES_PREF_KEY to settings.globalUpdateIncludeCategoryIds,
    ).forEach { (key, ids) ->
        // LNReader's built-in categories aren't Katari's; their novels are in Katari's Default (D093)
        val kept = ids.orEmpty().filter { it in categoryIds }
        if (kept.isNotEmpty()) write(key, StringSetPreferenceValue(kept.mapTo(linkedSetOf()) { it.toString() }))
    }
    settings.sortOrder?.let(::convertLibrarySort)
    settings.filter?.let { filter ->
        FILTERS[filter]?.let { (key, state) -> write(key, StringPreferenceValue(state.name)) }
    }
    settings.showDownloadBadges?.let { write("display_download_badge", BooleanPreferenceValue(it)) }
    settings.showUnreadBadges?.let { write("display_unread_badge", BooleanPreferenceValue(it)) }
    settings.showContinueReadingButton?.let { write("display_continue_reading_button", BooleanPreferenceValue(it)) }
    settings.showNumberOfNovels?.let { write("display_number_of_items", BooleanPreferenceValue(it)) }
    settings.displayMode?.let { mode ->
        DISPLAY_MODES[mode]?.let { write("pref_display_mode_library", StringPreferenceValue(it.serialize())) }
    }
    settings.novelsPerRow?.let { perRow ->
        COVER_SIZES[perRow]?.let { write("pref_library_cover_size", StringPreferenceValue(it.name)) }
    }
}

/**
 * Asking each time is Katari's "Always ask"; one of the user's categories carries by its id. LNReader's built-in
 * Default, which it means by no category, writes nothing, so Katari keeps asking (D089).
 */
private fun LnReaderSettings.convertDefaultCategory(settings: LnReaderLibrarySettings, categoryIds: Set<Long>) {
    if (settings.promptForCategoryOnAdd == null && settings.defaultCategoryId == null) return
    val id = settings.defaultCategoryId
    when {
        settings.promptForCategoryOnAdd == true ->
            write(LibraryPreferences.DEFAULT_CATEGORY_PREF_KEY, IntPreferenceValue(ALWAYS_ASK))
        id != null && id in categoryIds ->
            write(LibraryPreferences.DEFAULT_CATEGORY_PREF_KEY, IntPreferenceValue(id.toInt()))
        else -> leaveBehind(
            LnReaderSettingLabels.DEFAULT_CATEGORY,
            MR.strings.backup_conversion_lnreader_default_category_built_in,
        )
    }
}

private fun LnReaderSettings.convertLibrarySort(sortOrder: String) {
    val (column, direction) = sortOrder.split(' ').takeIf { it.size == 2 } ?: return unreadableSort()
    val sortDirection = when (direction) {
        "ASC" -> LibrarySort.Direction.Ascending
        "DESC" -> LibrarySort.Direction.Descending
        else -> return unreadableSort()
    }
    if (column == DOWNLOADED_COLUMN) {
        leaveBehind(Item(Name.Setting(LIBRARY_SORT_KEY), MR.strings.backup_conversion_lnreader_sort_downloaded))
        return
    }
    val type = SORT_TYPES[column] ?: return unreadableSort()
    write(LIBRARY_SORT_KEY, StringPreferenceValue(LibrarySort(type, sortDirection).serialize()))
}

private fun LnReaderSettings.unreadableSort() {
    leaveBehind(Item(Name.Setting(LIBRARY_SORT_KEY), MR.strings.backup_conversion_lnreader_unreadable))
}

private const val ALWAYS_ASK = -1
private const val LIBRARY_SORT_KEY = "library_sorting_mode"
private const val DOWNLOADED_COLUMN = "chaptersDownloaded"

// LibrarySortOrder's columns; LNReader sorts by date added by its novels' ids
private val SORT_TYPES = mapOf(
    "name" to LibrarySort.Type.Alphabetical,
    "chaptersUnread" to LibrarySort.Type.UnreadCount,
    "totalChapters" to LibrarySort.Type.TotalChapters,
    "id" to LibrarySort.Type.DateAdded,
    "lastReadAt" to LibrarySort.Type.LastRead,
    "lastUpdatedAt" to LibrarySort.Type.LastUpdate,
)

/*
 * LibraryFilter's conditions, one of which LNReader applies at a time. Its Unread is novels never read, and Started
 * the ones read: Katari's not-started filter and its opposite.
 */
private val FILTERS = mapOf(
    "(chaptersDownloaded > 0 OR isLocal = 1)" to ("pref_filter_library_downloaded_v2" to TriState.ENABLED_IS),
    "status LIKE 'Completed'" to ("pref_filter_library_completed_v2" to TriState.ENABLED_IS),
    "lastReadAt IS NULL" to (NOT_STARTED_FILTER_KEY to TriState.ENABLED_IS),
    "lastReadAt IS NOT NULL" to (NOT_STARTED_FILTER_KEY to TriState.ENABLED_NOT),
)

private const val NOT_STARTED_FILTER_KEY = "pref_filter_library_not_started"

// LNReader's DisplayModes by number
private val DISPLAY_MODES = mapOf(
    0 to LibraryDisplayMode.CompactGrid,
    1 to LibraryDisplayMode.ComfortableGrid,
    2 to LibraryDisplayMode.CoverOnlyGrid,
    3 to LibraryDisplayMode.List,
)

// LNReader's grid sizes, from extra small at five novels a row to extra large at one
private val COVER_SIZES = mapOf(
    5 to CoverSize.ExtraSmall,
    4 to CoverSize.Small,
    3 to CoverSize.Medium,
    2 to CoverSize.Large,
    1 to CoverSize.ExtraLarge,
)
