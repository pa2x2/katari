package eu.kanade.presentation.library.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import eu.kanade.presentation.library.grouping.showLibraryGroupingTabsLabel
import eu.kanade.tachiyomi.ui.library.LibrarySettingsViewModel
import mihon.presentation.kit.list.RowGroup
import mihon.presentation.kit.list.RowGroupGap
import mihon.presentation.kit.list.RowGroupHeader
import mihon.presentation.kit.list.SliderRow
import mihon.presentation.kit.list.ToggleRow
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.toggle
import tachiyomi.domain.library.model.CoverSize
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

@Composable
internal fun LibraryDisplayPage(
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
        RowGroupGap()
        RowGroup {
            row {
                val coverSizePreference = viewModel.libraryPreferences.coverSize
                val coverSize by coverSizePreference.collectAsState()
                SliderRow(
                    title = stringResource(MR.strings.pref_library_cover_size),
                    value = coverSize.ordinal,
                    valueRange = CoverSize.entries.indices,
                    onValueChange = { coverSizePreference.set(CoverSize.entries[it]) },
                    valueText = stringResource(coverSize.labelRes),
                )
            }
        }
    }

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
    )
}
