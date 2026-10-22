package eu.kanade.presentation.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import mihon.app.di.appGraph
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Group
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.list.CheckRow
import mihon.presentation.kit.list.ChoiceRow
import mihon.presentation.kit.list.RowGroup
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.list.SortRow
import mihon.presentation.kit.list.TriStateRow
import mihon.presentation.kit.menu.DropdownMenuItem
import mihon.presentation.kit.sheet.TabbedSheet
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.entry.model.Entry
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun EntryChapterSettingsDialog(
    onDismissRequest: () -> Unit,
    entry: Entry? = null,
    isMerged: Boolean = false,
    onDownloadFilterChanged: (TriState) -> Unit,
    onUnreadFilterChanged: (TriState) -> Unit,
    onBookmarkedFilterChanged: (TriState) -> Unit,
    scanlatorFilterActive: Boolean,
    onScanlatorFilterClicked: (() -> Unit)?,
    onSortModeChanged: (Long) -> Unit,
    onDisplayModeChanged: (Long) -> Unit,
    onSetAsDefault: (applyToExisting: Boolean) -> Unit,
    onResetToDefault: () -> Unit,
) {
    val context = LocalContext.current
    val presentation = entry?.type.entryTypePresentation()
    var showSetAsDefaultDialog by rememberSaveable { mutableStateOf(false) }
    if (showSetAsDefaultDialog) {
        SetAsDefaultDialog(
            presentation = presentation,
            onDismissRequest = { showSetAsDefaultDialog = false },
            onConfirmed = onSetAsDefault,
        )
    }

    val downloadedOnly = remember { context.appGraph.libraryPreferences.downloadedOnly.get() }

    TabbedSheet(
        onDismissRequest = onDismissRequest,
        tabTitles = listOf(
            stringResource(MR.strings.action_filter),
            stringResource(MR.strings.action_sort),
            stringResource(MR.strings.action_display),
        ),
        menu = { closeMenu ->
            DropdownMenuItem(
                label = stringResource(presentation.setSettingsAsDefaultLabel),
                onClick = {
                    showSetAsDefaultDialog = true
                    closeMenu()
                },
            )
            DropdownMenuItem(
                label = stringResource(MR.strings.action_reset),
                onClick = {
                    onResetToDefault()
                    closeMenu()
                },
            )
        },
    ) { page ->
        when (page) {
            0 -> {
                FilterPage(
                    downloadFilter = entry?.downloadedFilter ?: TriState.DISABLED,
                    onDownloadFilterChanged = onDownloadFilterChanged
                        .takeUnless { downloadedOnly },
                    unreadFilter = entry?.unreadFilter ?: TriState.DISABLED,
                    onUnreadFilterChanged = onUnreadFilterChanged,
                    bookmarkedFilter = entry?.bookmarkedFilter ?: TriState.DISABLED,
                    onBookmarkedFilterChanged = onBookmarkedFilterChanged,
                    scanlatorFilterActive = scanlatorFilterActive,
                    onScanlatorFilterClicked = onScanlatorFilterClicked,
                    unconsumedFilterLabel = presentation.filterUnconsumedLabel,
                )
            }
            1 -> {
                SortPage(
                    presentation = presentation,
                    sortingMode = entry?.sorting ?: 0,
                    sortDescending = entry?.sortDescending() ?: false,
                    onItemSelected = onSortModeChanged,
                )
            }
            2 -> {
                DisplayPage(
                    presentation = presentation,
                    displayMode = entry?.displayMode ?: 0,
                    onItemSelected = onDisplayModeChanged,
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.FilterPage(
    downloadFilter: TriState,
    onDownloadFilterChanged: ((TriState) -> Unit)?,
    unreadFilter: TriState,
    onUnreadFilterChanged: (TriState) -> Unit,
    bookmarkedFilter: TriState,
    onBookmarkedFilterChanged: (TriState) -> Unit,
    scanlatorFilterActive: Boolean,
    onScanlatorFilterClicked: (() -> Unit)?,
    unconsumedFilterLabel: dev.icerock.moko.resources.StringResource,
) {
    RowGroup {
        row {
            TriStateRow(
                title = stringResource(MR.strings.label_downloaded),
                state = downloadFilter,
                onStateChange = onDownloadFilterChanged,
            )
        }
        row {
            TriStateRow(
                title = stringResource(unconsumedFilterLabel),
                state = unreadFilter,
                onStateChange = onUnreadFilterChanged,
            )
        }
        row {
            TriStateRow(
                title = stringResource(MR.strings.action_filter_bookmarked),
                state = bookmarkedFilter,
                onStateChange = onBookmarkedFilterChanged,
            )
        }
        if (onScanlatorFilterClicked != null) {
            row {
                ScanlatorFilterItem(
                    active = scanlatorFilterActive,
                    onClick = onScanlatorFilterClicked,
                )
            }
        }
    }
}

@Composable
private fun ScanlatorFilterItem(
    active: Boolean,
    onClick: () -> Unit,
) {
    SettingsRow(
        title = stringResource(MR.strings.scanlator),
        leading = {
            Icon(
                imageVector = MaterialSymbols.Rounded.Group,
                contentDescription = null,
                tone = if (active) ContentTone.Active else ContentTone.Inherit,
            )
        },
        onClick = onClick,
    )
}

@Composable
private fun ColumnScope.SortPage(
    presentation: EntryTypePresentation,
    sortingMode: Long,
    sortDescending: Boolean,
    onItemSelected: (Long) -> Unit,
) {
    val options = listOf(
        MR.strings.sort_by_source to Entry.CHAPTER_SORTING_SOURCE,
        presentation.downloadNumberSortLabel to Entry.CHAPTER_SORTING_NUMBER,
        MR.strings.sort_by_upload_date to Entry.CHAPTER_SORTING_UPLOAD_DATE,
        MR.strings.action_sort_alpha to Entry.CHAPTER_SORTING_ALPHABET,
    )
    RowGroup {
        options.forEach { (titleRes, mode) ->
            row(key = mode) {
                SortRow(
                    title = stringResource(titleRes),
                    descending = sortDescending.takeIf { sortingMode == mode },
                    onClick = { onItemSelected(mode) },
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.DisplayPage(
    presentation: EntryTypePresentation,
    displayMode: Long,
    onItemSelected: (Long) -> Unit,
) {
    val options = listOf(
        MR.strings.show_title to Entry.CHAPTER_DISPLAY_NAME,
        presentation.childNumberSettingLabel to Entry.CHAPTER_DISPLAY_NUMBER,
    )
    RowGroup {
        options.forEach { (titleRes, mode) ->
            row(key = mode) {
                ChoiceRow(
                    title = stringResource(titleRes),
                    selected = displayMode == mode,
                    onClick = { onItemSelected(mode) },
                )
            }
        }
    }
}

@Composable
private fun SetAsDefaultDialog(
    presentation: EntryTypePresentation,
    onDismissRequest: () -> Unit,
    onConfirmed: (optionalChecked: Boolean) -> Unit,
) {
    var optionalChecked by rememberSaveable { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(presentation.settingsTitle),
        confirm = DialogAction(stringResource(presentation.setSettingsAsDefaultLabel), onClick = {
            onConfirmed(optionalChecked)
            onDismissRequest()
        }),
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onDismissRequest),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m)) {
            Text(text = stringResource(presentation.confirmSetSettingsAsDefaultLabel))
            CheckRow(
                title = stringResource(presentation.alsoSetSettingsForLibraryLabel),
                checked = optionalChecked,
                onCheckedChange = { optionalChecked = it },
            )
        }
    }
}
