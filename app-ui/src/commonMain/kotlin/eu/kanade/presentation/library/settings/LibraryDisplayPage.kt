package eu.kanade.presentation.library.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.katari.coverlayout.CoverLayoutSettings
import app.katari.presentation.kit.layout.CoverSizing
import app.katari.presentation.kit.list.RowGroup
import app.katari.presentation.kit.list.RowGroupHeader
import app.katari.presentation.kit.list.ToggleRow
import eu.kanade.presentation.library.grouping.showLibraryGroupingTabsLabel
import eu.kanade.tachiyomi.ui.library.LibrarySettingsViewModel
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.toggle
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

/** The library's part of the Display panel: its display mode and cover size. */
@Composable
internal fun LibraryCoverLayoutSettings(viewModel: LibrarySettingsViewModel, sizing: CoverSizing) {
    val displayMode by viewModel.libraryPreferences.displayMode.collectAsState()
    CoverLayoutSettings(
        displayMode = displayMode,
        onDisplayModeChange = viewModel::setDisplayMode,
        sizing = sizing,
        displayModeSetting = viewModel.libraryPreferences.displayMode,
        coverSizeSetting = viewModel.libraryPreferences.coverSize,
    )
}

@Composable
internal fun LibraryDisplayPage(
    viewModel: LibrarySettingsViewModel,
    sizing: CoverSizing,
) {
    LibraryCoverLayoutSettings(viewModel, sizing)

    val pinnedDisplayStyle by viewModel.libraryPreferences.pinnedDisplayStyle.collectAsState()
    LibraryPinnedStyleChooser(
        selectedStyle = pinnedDisplayStyle,
        onStyleSelected = viewModel::setPinnedDisplayStyle,
        setting = viewModel.libraryPreferences.pinnedDisplayStyle,
    )

    val preferences = viewModel.libraryPreferences
    RowGroupHeader(stringResource(MR.strings.overlay_header))
    RowGroup {
        row { PreferenceToggleRow(stringResource(MR.strings.action_display_download_badge), preferences.downloadBadge) }
        row { PreferenceToggleRow(stringResource(MR.strings.action_display_unseen_badge), preferences.unreadBadge) }
        row { PreferenceToggleRow(stringResource(MR.strings.action_display_local_badge), preferences.localBadge) }
        row {
            PreferenceToggleRow(stringResource(MR.strings.action_display_language_badge), preferences.languageBadge)
        }
        row {
            PreferenceToggleRow(
                stringResource(MR.strings.action_display_entry_type_badge),
                preferences.entryTypeBadge,
            )
        }
        row { PreferenceToggleRow(stringResource(MR.strings.action_display_merged_badge), preferences.mergedBadge) }
    }

    RowGroupHeader(stringResource(MR.strings.library_cover_actions_header))
    RowGroup {
        row {
            PreferenceToggleRow(
                stringResource(MR.strings.action_display_show_continue_button),
                preferences.showContinueReadingButton,
            )
        }
    }

    RowGroupHeader(stringResource(MR.strings.tabs_header))
    RowGroup {
        row {
            val grouping by preferences.grouping.collectAsState()
            PreferenceToggleRow(showLibraryGroupingTabsLabel(grouping), preferences.categoryTabs)
        }
        row {
            PreferenceToggleRow(
                stringResource(MR.strings.action_display_show_number_of_items),
                preferences.categoryNumberOfItems,
            )
        }
    }
}

@Composable
private fun PreferenceToggleRow(title: String, preference: Preference<Boolean>) {
    val checked by preference.collectAsState()
    ToggleRow(
        title = title,
        checked = checked,
        onCheckedChange = { preference.toggle() },
        setting = preference,
    )
}
