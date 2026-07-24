package eu.kanade.presentation.library

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import eu.kanade.presentation.components.TabbedDialog
import eu.kanade.presentation.components.TabbedDialogPaddings
import eu.kanade.tachiyomi.ui.library.LibrarySettingsScreenModel
import eu.kanade.tachiyomi.util.system.isReleaseBuildType
import mihon.entry.interactions.EntryLibraryFilterAvailability
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.model.LibraryGroupType
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.domain.library.model.effectiveLibrarySort
import tachiyomi.domain.library.model.sort
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.BaseSortItem
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.HeadingItem
import tachiyomi.presentation.core.components.RadioItem
import tachiyomi.presentation.core.components.SettingsChipRow
import tachiyomi.presentation.core.components.SliderItem
import tachiyomi.presentation.core.components.SortItem
import tachiyomi.presentation.core.components.TriStateItem
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

@Composable
fun LibrarySettingsDialog(
    onDismissRequest: () -> Unit,
    screenModel: LibrarySettingsScreenModel,
    category: Category?,
    filterAvailability: EntryLibraryFilterAvailability,
) {
    TabbedDialog(
        onDismissRequest = onDismissRequest,
        tabTitles = listOf(
            stringResource(MR.strings.action_filter),
            stringResource(MR.strings.action_sort),
            stringResource(MR.strings.action_display),
            stringResource(MR.strings.action_group),
        ),
    ) { page ->
        Column(
            modifier = Modifier
                .padding(vertical = TabbedDialogPaddings.Vertical)
                .verticalScroll(rememberScrollState()),
        ) {
            when (page) {
                0 -> FilterPage(
                    screenModel = screenModel,
                    filterAvailability = filterAvailability,
                )
                1 -> SortPage(
                    category = category,
                    screenModel = screenModel,
                    progressSummaryAvailable = filterAvailability.progressSummary.isAvailable,
                )
                2 -> DisplayPage(
                    screenModel = screenModel,
                )
                3 -> GroupPage(
                    screenModel = screenModel,
                )
            }
        }
    }
}

@Composable
private fun FilterPage(
    screenModel: LibrarySettingsScreenModel,
    filterAvailability: EntryLibraryFilterAvailability,
) {
    val filterDownloaded by screenModel.libraryPreferences.filterDownloaded.collectAsState()
    val downloadedOnly by screenModel.libraryPreferences.downloadedOnly.collectAsState()
    val autoUpdateEntryRestrictions by screenModel.libraryPreferences.autoUpdateEntryRestrictions.collectAsState()

    TriStateItem(
        label = stringResource(MR.strings.label_downloaded),
        state = if (downloadedOnly) {
            TriState.ENABLED_IS
        } else {
            filterDownloaded
        },
        enabled = !downloadedOnly,
        onClick = { screenModel.toggleFilter(LibraryPreferences::filterDownloaded) },
    )
    if (filterAvailability.progressSummary.isAvailable) {
        val filterUnread by screenModel.libraryPreferences.filterUnread.collectAsState()
        TriStateItem(
            label = stringResource(MR.strings.action_filter_unconsumed),
            state = filterUnread,
            onClick = { screenModel.toggleFilter(LibraryPreferences::filterUnread) },
        )
        val filterNotStarted by screenModel.libraryPreferences.filterNotStarted.collectAsState()
        TriStateItem(
            label = stringResource(MR.strings.label_not_started),
            state = filterNotStarted,
            onClick = { screenModel.toggleFilter(LibraryPreferences::filterNotStarted) },
        )
    }
    if (filterAvailability.bookmarking.isAvailable) {
        val filterBookmarked by screenModel.libraryPreferences.filterBookmarked.collectAsState()
        TriStateItem(
            label = stringResource(MR.strings.action_filter_bookmarked),
            state = filterBookmarked,
            onClick = { screenModel.toggleFilter(LibraryPreferences::filterBookmarked) },
        )
    }
    val filterCompleted by screenModel.libraryPreferences.filterCompleted.collectAsState()
    TriStateItem(
        label = stringResource(MR.strings.completed),
        state = filterCompleted,
        onClick = { screenModel.toggleFilter(LibraryPreferences::filterCompleted) },
    )
    // TODO: re-enable when custom intervals are ready for stable
    if (
        (!isReleaseBuildType) &&
        filterAvailability.outsideReleasePeriod.isAvailable &&
        LibraryPreferences.ENTRY_OUTSIDE_RELEASE_PERIOD in autoUpdateEntryRestrictions
    ) {
        val filterIntervalCustom by screenModel.libraryPreferences.filterIntervalCustom.collectAsState()
        TriStateItem(
            label = stringResource(MR.strings.action_filter_interval_custom),
            state = filterIntervalCustom,
            onClick = { screenModel.toggleFilter(LibraryPreferences::filterIntervalCustom) },
        )
    }

    val trackingServices by screenModel.trackingServicesFlow.collectAsState()
    when (trackingServices.size) {
        0 -> {
            // No trackers
        }
        1 -> {
            val service = trackingServices[0]
            val filterTracker by screenModel.libraryPreferences
                .filterTracking(service.id.value.toInt())
                .collectAsState()
            TriStateItem(
                label = stringResource(MR.strings.action_filter_tracked),
                state = filterTracker,
                onClick = { screenModel.toggleTracker(service.id.value.toInt()) },
            )
        }
        else -> {
            HeadingItem(MR.strings.action_filter_tracked)
            trackingServices.map { service ->
                val filterTracker by screenModel.libraryPreferences
                    .filterTracking(service.id.value.toInt())
                    .collectAsState()
                TriStateItem(
                    label = service.name,
                    state = filterTracker,
                    onClick = { screenModel.toggleTracker(service.id.value.toInt()) },
                )
            }
        }
    }
}

@Composable
private fun SortPage(
    category: Category?,
    screenModel: LibrarySettingsScreenModel,
    progressSummaryAvailable: Boolean,
) {
    val trackingServices by screenModel.trackingServicesFlow.collectAsState()
    val globalSort by screenModel.libraryPreferences.sortingMode.collectAsState()
    val currentSort = category.effectiveLibrarySort(globalSort)
    val sortingMode = currentSort.type
    val sortDescending = !currentSort.isAscending

    val options = remember(trackingServices.isEmpty(), progressSummaryAvailable) {
        val trackerMeanPair = if (trackingServices.isNotEmpty()) {
            MR.strings.action_sort_tracker_score to LibrarySort.Type.TrackerMean
        } else {
            null
        }
        listOfNotNull(
            MR.strings.action_sort_alpha to LibrarySort.Type.Alphabetical,
            (MR.strings.action_sort_total to LibrarySort.Type.TotalChapters)
                .takeIf { progressSummaryAvailable },
            (MR.strings.action_sort_last_read to LibrarySort.Type.LastRead)
                .takeIf { progressSummaryAvailable },
            MR.strings.action_sort_last_manga_update to LibrarySort.Type.LastUpdate,
            (MR.strings.action_sort_unread_count to LibrarySort.Type.UnreadCount)
                .takeIf { progressSummaryAvailable },
            MR.strings.action_sort_latest_chapter to LibrarySort.Type.LatestChapter,
            MR.strings.action_sort_chapter_fetch_date to LibrarySort.Type.ChapterFetchDate,
            MR.strings.action_sort_date_added to LibrarySort.Type.DateAdded,
            trackerMeanPair,
            MR.strings.action_sort_random to LibrarySort.Type.Random,
        )
    }

    options.forEach { (titleRes, mode) ->
        if (mode == LibrarySort.Type.Random) {
            BaseSortItem(
                label = stringResource(titleRes),
                icon = Icons.Default.Refresh
                    .takeIf { sortingMode == LibrarySort.Type.Random },
                onClick = {
                    screenModel.setSort(category, mode, LibrarySort.Direction.Ascending)
                },
            )
            return@forEach
        }
        SortItem(
            label = stringResource(titleRes),
            sortDescending = sortDescending.takeIf { sortingMode == mode },
            onClick = {
                val isTogglingDirection = sortingMode == mode
                val direction = when {
                    isTogglingDirection -> if (sortDescending) {
                        LibrarySort.Direction.Ascending
                    } else {
                        LibrarySort.Direction.Descending
                    }
                    else -> if (sortDescending) {
                        LibrarySort.Direction.Descending
                    } else {
                        LibrarySort.Direction.Ascending
                    }
                }
                screenModel.setSort(category, mode, direction)
            },
        )
    }
}

private val displayModes = listOf(
    MR.strings.action_display_grid to LibraryDisplayMode.CompactGrid,
    MR.strings.action_display_comfortable_grid to LibraryDisplayMode.ComfortableGrid,
    MR.strings.action_display_comfortable_list to LibraryDisplayMode.ComfortableList,
    MR.strings.action_display_cover_only_grid to LibraryDisplayMode.CoverOnlyGrid,
    MR.strings.action_display_list to LibraryDisplayMode.List,
)

@Composable
private fun DisplayPage(
    screenModel: LibrarySettingsScreenModel,
) {
    val displayMode by screenModel.libraryPreferences.displayMode.collectAsState()
    SettingsChipRow(MR.strings.action_display_mode) {
        displayModes.forEach { (titleRes, mode) ->
            FilterChip(
                selected = displayMode == mode,
                onClick = { screenModel.setDisplayMode(mode) },
                label = { Text(stringResource(titleRes)) },
            )
        }
    }

    if (displayMode != LibraryDisplayMode.List && displayMode != LibraryDisplayMode.ComfortableList) {
        val configuration = LocalConfiguration.current
        val columnPreference = remember {
            if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                screenModel.libraryPreferences.landscapeColumns
            } else {
                screenModel.libraryPreferences.portraitColumns
            }
        }

        val columns by columnPreference.collectAsState()
        SliderItem(
            value = columns,
            valueRange = 0..10,
            label = stringResource(MR.strings.pref_library_columns),
            valueString = if (columns > 0) {
                columns.toString()
            } else {
                stringResource(MR.strings.label_auto)
            },
            onChange = columnPreference::set,
            pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
    }

    HeadingItem(MR.strings.overlay_header)
    CheckboxItem(
        label = stringResource(MR.strings.action_display_download_badge),
        pref = screenModel.libraryPreferences.downloadBadge,
    )
    CheckboxItem(
        label = stringResource(MR.strings.action_display_unseen_badge),
        pref = screenModel.libraryPreferences.unreadBadge,
    )
    CheckboxItem(
        label = stringResource(MR.strings.action_display_local_badge),
        pref = screenModel.libraryPreferences.localBadge,
    )
    CheckboxItem(
        label = stringResource(MR.strings.action_display_language_badge),
        pref = screenModel.libraryPreferences.languageBadge,
    )
    CheckboxItem(
        label = stringResource(MR.strings.action_display_entry_type_badge),
        pref = screenModel.libraryPreferences.entryTypeBadge,
    )
    CheckboxItem(
        label = stringResource(MR.strings.action_display_show_continue_button),
        pref = screenModel.libraryPreferences.showContinueReadingButton,
    )

    HeadingItem(MR.strings.tabs_header)
    val groupType by screenModel.libraryPreferences.groupType.collectAsState()
    CheckboxItem(
        label = stringResource(
            when (groupType) {
                LibraryGroupType.Category -> MR.strings.action_display_show_tabs
                LibraryGroupType.Type -> MR.strings.action_display_show_type_tabs
                LibraryGroupType.Extension -> MR.strings.action_display_show_extension_tabs
                LibraryGroupType.TypeCategory,
                LibraryGroupType.CategoryType,
                LibraryGroupType.ExtensionCategory,
                LibraryGroupType.CategoryExtension,
                -> MR.strings.action_display_show_group_tabs
            },
        ),
        pref = screenModel.libraryPreferences.categoryTabs,
    )
    CheckboxItem(
        label = stringResource(MR.strings.action_display_show_number_of_items),
        pref = screenModel.libraryPreferences.categoryNumberOfItems,
    )
}

@Composable
private fun GroupPage(
    screenModel: LibrarySettingsScreenModel,
) {
    val groupState by screenModel.libraryPreferences.groupType.collectAsState()

    val options = remember {
        listOfNotNull(
            MR.strings.action_group_category to LibraryGroupType.Category,
            MR.strings.action_group_type to LibraryGroupType.Type,
            MR.strings.action_group_extension to LibraryGroupType.Extension,
            MR.strings.action_group_type_category to LibraryGroupType.TypeCategory,
            MR.strings.action_group_category_type to LibraryGroupType.CategoryType,
            MR.strings.action_group_extension_category to LibraryGroupType.ExtensionCategory,
            MR.strings.action_group_category_extension to LibraryGroupType.CategoryExtension,
        )
    }

    options.forEach { (titleRes, mode) ->
        RadioItem(
            label = stringResource(titleRes),
            selected = mode == groupState,
            onClick = {
                screenModel.setGroup(mode)
            },
        )
    }
}
