package eu.kanade.presentation.library.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.library.LibrarySettingsViewModel
import eu.kanade.tachiyomi.util.system.isReleaseBuildType
import mihon.entry.interactions.library.EntryLibraryFilterAvailability
import mihon.presentation.kit.list.SectionHeader
import mihon.presentation.kit.list.TriStateRow
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
    val filterDownloaded by viewModel.libraryPreferences.filterDownloaded.collectAsState()
    val downloadedOnly by viewModel.libraryPreferences.downloadedOnly.collectAsState()
    val updateSkipRules by viewModel.libraryPreferences.updateSkipRules.collectAsState()

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
    if (filterAvailability.progressSummary.isAvailable) {
        val filterUnread by viewModel.libraryPreferences.filterUnread.collectAsState()
        // Each type names its own unconsumed state; a mixed library lists every distinct name.
        val unconsumedLabel = filterAvailability.progressSummary.applicableTypes
            .sortedBy(EntryType::ordinal)
            .map { it.entryTypePresentation().filterUnconsumedLabel }
            .distinct()
            .map { stringResource(it) }
            .joinToString(separator = " / ")
        TriStateRow(
            title = unconsumedLabel,
            state = filterUnread,
            onStateChange = { viewModel.toggleFilter(LibraryPreferences::filterUnread) },
        )
        val filterNotStarted by viewModel.libraryPreferences.filterNotStarted.collectAsState()
        TriStateRow(
            title = stringResource(MR.strings.label_not_started),
            state = filterNotStarted,
            onStateChange = { viewModel.toggleFilter(LibraryPreferences::filterNotStarted) },
        )
    }
    if (filterAvailability.bookmarking.isAvailable) {
        val filterBookmarked by viewModel.libraryPreferences.filterBookmarked.collectAsState()
        TriStateRow(
            title = stringResource(MR.strings.action_filter_bookmarked),
            state = filterBookmarked,
            onStateChange = { viewModel.toggleFilter(LibraryPreferences::filterBookmarked) },
        )
    }
    val filterCompleted by viewModel.libraryPreferences.filterCompleted.collectAsState()
    TriStateRow(
        title = stringResource(MR.strings.completed),
        state = filterCompleted,
        onStateChange = { viewModel.toggleFilter(LibraryPreferences::filterCompleted) },
    )
    // TODO: re-enable when custom intervals are ready for stable
    if (
        (!isReleaseBuildType) &&
        filterAvailability.outsideReleasePeriod.isAvailable &&
        LibraryPreferences.SKIP_OUTSIDE_RELEASE_PERIOD in updateSkipRules
    ) {
        val filterIntervalCustom by viewModel.libraryPreferences.filterIntervalCustom.collectAsState()
        TriStateRow(
            title = stringResource(MR.strings.action_filter_interval_custom),
            state = filterIntervalCustom,
            onStateChange = { viewModel.toggleFilter(LibraryPreferences::filterIntervalCustom) },
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
            TriStateRow(
                title = stringResource(MR.strings.action_filter_tracked),
                state = filterTracker,
                onStateChange = { viewModel.toggleTracker(service.id.value.toInt()) },
            )
        }
        else -> {
            SectionHeader(stringResource(MR.strings.action_filter_tracked))
            trackingServices.map { service ->
                val filterTracker by viewModel.libraryPreferences
                    .filterTracking(service.id.value.toInt())
                    .collectAsState()
                TriStateRow(
                    title = service.name,
                    state = filterTracker,
                    onStateChange = { viewModel.toggleTracker(service.id.value.toInt()) },
                )
            }
        }
    }
}
