package eu.kanade.presentation.library.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.katari.entry.interactions.library.EntryLibraryFilterAvailability
import app.katari.presentation.kit.list.RowGroup
import app.katari.presentation.kit.list.TriStateRow
import app.katari.sync.client.setting.scope.MenuSettings
import app.katari.sync.ui.choice.MenuSyncHeader
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.library.LibrarySettingsViewModel
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

@Composable
internal fun LibraryFilterPage(
    viewModel: LibrarySettingsViewModel,
    filterAvailability: EntryLibraryFilterAvailability,
) {
    val updateSkipRules by viewModel.libraryPreferences.updateSkipRules.collectAsState()
    // Each type names its own unconsumed state; a mixed library lists every distinct name.
    val unconsumedLabel = filterAvailability.progressSummary.applicableTypes
        .sortedBy(EntryType::ordinal)
        .map { it.entryTypePresentation().filterUnconsumedLabel }
        .distinct()
        .map { stringResource(it) }
        .joinToString(separator = " / ")

    MenuSyncHeader(MenuSettings.LIBRARY_FILTERS)
    RowGroup {
        row {
            val filterDownloaded by viewModel.libraryPreferences.filterDownloaded.collectAsState()
            val downloadedOnly by viewModel.libraryPreferences.downloadedOnly.collectAsState()
            TriStateRow(
                title = stringResource(MR.strings.label_downloaded),
                state = if (downloadedOnly) {
                    TriState.ENABLED_IS
                } else {
                    filterDownloaded
                },
                enabled = !downloadedOnly,
                onStateChange = { viewModel.toggleFilter(LibraryPreferences::filterDownloaded) },
            )
        }
        if (filterAvailability.progressSummary.isAvailable) {
            row {
                val filterUnread by viewModel.libraryPreferences.filterUnread.collectAsState()
                TriStateRow(
                    title = unconsumedLabel,
                    state = filterUnread,
                    onStateChange = { viewModel.toggleFilter(LibraryPreferences::filterUnread) },
                )
            }
            row {
                val filterNotStarted by viewModel.libraryPreferences.filterNotStarted.collectAsState()
                TriStateRow(
                    title = stringResource(MR.strings.label_not_started),
                    state = filterNotStarted,
                    onStateChange = { viewModel.toggleFilter(LibraryPreferences::filterNotStarted) },
                )
            }
        }
        if (filterAvailability.bookmarking.isAvailable) {
            row {
                val filterBookmarked by viewModel.libraryPreferences.filterBookmarked.collectAsState()
                TriStateRow(
                    title = stringResource(MR.strings.action_filter_bookmarked),
                    state = filterBookmarked,
                    onStateChange = { viewModel.toggleFilter(LibraryPreferences::filterBookmarked) },
                )
            }
        }
        row {
            val filterCompleted by viewModel.libraryPreferences.filterCompleted.collectAsState()
            TriStateRow(
                title = stringResource(MR.strings.completed),
                state = filterCompleted,
                onStateChange = { viewModel.toggleFilter(LibraryPreferences::filterCompleted) },
            )
        }
        if (
            viewModel.releasePeriodFilter.isOffered &&
            filterAvailability.outsideReleasePeriod.isAvailable &&
            LibraryPreferences.SKIP_OUTSIDE_RELEASE_PERIOD in updateSkipRules
        ) {
            row {
                val filterIntervalCustom by viewModel.libraryPreferences.filterIntervalCustom.collectAsState()
                TriStateRow(
                    title = stringResource(MR.strings.action_filter_interval_custom),
                    state = filterIntervalCustom,
                    onStateChange = { viewModel.toggleFilter(LibraryPreferences::filterIntervalCustom) },
                )
            }
        }
    }
}
