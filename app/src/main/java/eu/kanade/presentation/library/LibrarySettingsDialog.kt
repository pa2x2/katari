package eu.kanade.presentation.library

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import eu.kanade.presentation.components.TabbedDialog
import eu.kanade.presentation.components.TabbedDialogPaddings
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.presentation.library.components.LibraryDisplayModeChooser
import eu.kanade.presentation.library.components.LibraryPinnedStyleChooser
import eu.kanade.presentation.library.grouping.LibraryGroupingEditor
import eu.kanade.presentation.library.grouping.showLibraryGroupingTabsLabel
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.library.LibrarySettingsViewModel
import eu.kanade.tachiyomi.util.system.isReleaseBuildType
import mihon.entry.interactions.library.EntryLibraryFilterAvailability
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Refresh
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.domain.library.model.effectiveLibrarySort
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.BaseSortItem
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.HeadingItem
import tachiyomi.presentation.core.components.SliderItem
import tachiyomi.presentation.core.components.SortItem
import tachiyomi.presentation.core.components.TriStateItem
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

@Composable
fun LibrarySettingsDialog(
    onDismissRequest: () -> Unit,
    viewModel: LibrarySettingsViewModel,
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
                    viewModel = viewModel,
                    filterAvailability = filterAvailability,
                )
                1 -> SortPage(
                    category = category,
                    viewModel = viewModel,
                    progressSummaryAvailable = filterAvailability.progressSummary.isAvailable,
                )
                2 -> DisplayPage(
                    viewModel = viewModel,
                )
                3 -> GroupPage(
                    viewModel = viewModel,
                )
            }
        }
    }
}

@Composable
private fun FilterPage(
    viewModel: LibrarySettingsViewModel,
    filterAvailability: EntryLibraryFilterAvailability,
) {
    val filterDownloaded by viewModel.libraryPreferences.filterDownloaded.collectAsState()
    val downloadedOnly by viewModel.libraryPreferences.downloadedOnly.collectAsState()
    val updateSkipRules by viewModel.libraryPreferences.updateSkipRules.collectAsState()

    TriStateItem(
        label = stringResource(MR.strings.label_downloaded),
        state = if (downloadedOnly) {
            TriState.ENABLED_IS
        } else {
            filterDownloaded
        },
        enabled = !downloadedOnly,
        onClick = { viewModel.toggleFilter(LibraryPreferences::filterDownloaded) },
    )
    if (filterAvailability.progressSummary.isAvailable) {
        val filterUnread by viewModel.libraryPreferences.filterUnread.collectAsState()
        // Each type names its own unconsumed state; a mixed library lists every distinct name.
        val unconsumedLabel = filterAvailability.progressSummary.applicableTypes
            .sortedBy(EntryType::ordinal)
            .map { it.entryTypePresentation().filterUnconsumedLabel }
            .distinct()
            .map { stringResource(it) }
            .joinToString(separator = " / ")
        TriStateItem(
            label = unconsumedLabel,
            state = filterUnread,
            onClick = { viewModel.toggleFilter(LibraryPreferences::filterUnread) },
        )
        val filterNotStarted by viewModel.libraryPreferences.filterNotStarted.collectAsState()
        TriStateItem(
            label = stringResource(MR.strings.label_not_started),
            state = filterNotStarted,
            onClick = { viewModel.toggleFilter(LibraryPreferences::filterNotStarted) },
        )
    }
    if (filterAvailability.bookmarking.isAvailable) {
        val filterBookmarked by viewModel.libraryPreferences.filterBookmarked.collectAsState()
        TriStateItem(
            label = stringResource(MR.strings.action_filter_bookmarked),
            state = filterBookmarked,
            onClick = { viewModel.toggleFilter(LibraryPreferences::filterBookmarked) },
        )
    }
    val filterCompleted by viewModel.libraryPreferences.filterCompleted.collectAsState()
    TriStateItem(
        label = stringResource(MR.strings.completed),
        state = filterCompleted,
        onClick = { viewModel.toggleFilter(LibraryPreferences::filterCompleted) },
    )
    // TODO: re-enable when custom intervals are ready for stable
    if (
        (!isReleaseBuildType) &&
        filterAvailability.outsideReleasePeriod.isAvailable &&
        LibraryPreferences.SKIP_OUTSIDE_RELEASE_PERIOD in updateSkipRules
    ) {
        val filterIntervalCustom by viewModel.libraryPreferences.filterIntervalCustom.collectAsState()
        TriStateItem(
            label = stringResource(MR.strings.action_filter_interval_custom),
            state = filterIntervalCustom,
            onClick = { viewModel.toggleFilter(LibraryPreferences::filterIntervalCustom) },
        )
    }

    val trackingServices by viewModel.trackingServicesFlow.collectAsState()
    when (trackingServices.size) {
        0 -> {
            // No trackers
        }
        1 -> {
            val service = trackingServices[0]
            val filterTracker by viewModel.libraryPreferences
                .filterTracking(service.id.value.toInt())
                .collectAsState()
            TriStateItem(
                label = stringResource(MR.strings.action_filter_tracked),
                state = filterTracker,
                onClick = { viewModel.toggleTracker(service.id.value.toInt()) },
            )
        }
        else -> {
            HeadingItem(MR.strings.action_filter_tracked)
            trackingServices.map { service ->
                val filterTracker by viewModel.libraryPreferences
                    .filterTracking(service.id.value.toInt())
                    .collectAsState()
                TriStateItem(
                    label = service.name,
                    state = filterTracker,
                    onClick = { viewModel.toggleTracker(service.id.value.toInt()) },
                )
            }
        }
    }
}

@Composable
private fun SortPage(
    category: Category?,
    viewModel: LibrarySettingsViewModel,
    progressSummaryAvailable: Boolean,
) {
    val trackingServices by viewModel.trackingServicesFlow.collectAsState()
    val globalSort by viewModel.libraryPreferences.sortingMode.collectAsState()
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
                icon = MaterialSymbols.Rounded.Refresh
                    .takeIf { sortingMode == LibrarySort.Type.Random },
                onClick = {
                    viewModel.setSort(category, mode, LibrarySort.Direction.Ascending)
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
                viewModel.setSort(category, mode, direction)
            },
        )
    }
}

@Composable
private fun DisplayPage(
    viewModel: LibrarySettingsViewModel,
) {
    val displayMode by viewModel.libraryPreferences.displayMode.collectAsState()
    LibraryDisplayModeChooser(
        selectedMode = displayMode,
        onModeSelected = viewModel::setDisplayMode,
    )

    val pinnedDisplayStyle by viewModel.libraryPreferences.pinnedDisplayStyle.collectAsState()
    LibraryPinnedStyleChooser(
        selectedStyle = pinnedDisplayStyle,
        onStyleSelected = viewModel::setPinnedDisplayStyle,
    )

    if (displayMode != LibraryDisplayMode.List && displayMode != LibraryDisplayMode.ComfortableList) {
        val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        val columnPreference = remember(isLandscape) {
            if (isLandscape) {
                viewModel.libraryPreferences.landscapeColumns
            } else {
                viewModel.libraryPreferences.portraitColumns
            }
        }

        val columns by columnPreference.collectAsState()
        SliderItem(
            value = columns,
            valueRange = 0..10,
            label = stringResource(
                MR.strings.pref_library_columns_for_orientation,
                stringResource(if (isLandscape) MR.strings.landscape else MR.strings.portrait),
            ),
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
        pref = viewModel.libraryPreferences.downloadBadge,
    )
    CheckboxItem(
        label = stringResource(MR.strings.action_display_unseen_badge),
        pref = viewModel.libraryPreferences.unreadBadge,
    )
    CheckboxItem(
        label = stringResource(MR.strings.action_display_local_badge),
        pref = viewModel.libraryPreferences.localBadge,
    )
    CheckboxItem(
        label = stringResource(MR.strings.action_display_language_badge),
        pref = viewModel.libraryPreferences.languageBadge,
    )
    CheckboxItem(
        label = stringResource(MR.strings.action_display_entry_type_badge),
        pref = viewModel.libraryPreferences.entryTypeBadge,
    )
    CheckboxItem(
        label = stringResource(MR.strings.action_display_merged_badge),
        pref = viewModel.libraryPreferences.mergedBadge,
    )

    HeadingItem(MR.strings.library_cover_actions_header)
    CheckboxItem(
        label = stringResource(MR.strings.action_display_show_continue_button),
        pref = viewModel.libraryPreferences.showContinueReadingButton,
    )

    HeadingItem(MR.strings.tabs_header)
    val grouping by viewModel.libraryPreferences.grouping.collectAsState()
    CheckboxItem(
        label = showLibraryGroupingTabsLabel(grouping),
        pref = viewModel.libraryPreferences.categoryTabs,
    )
    CheckboxItem(
        label = stringResource(MR.strings.action_display_show_number_of_items),
        pref = viewModel.libraryPreferences.categoryNumberOfItems,
    )
}

@Composable
private fun GroupPage(
    viewModel: LibrarySettingsViewModel,
) {
    val grouping by viewModel.libraryPreferences.grouping.collectAsState()
    HeadingItem(MR.strings.library_grouping_hierarchy)
    LibraryGroupingEditor(
        grouping = grouping,
        onGroupingChange = viewModel::setGrouping,
        modifier = Modifier.padding(horizontal = TabbedDialogPaddings.Horizontal),
    )
}
