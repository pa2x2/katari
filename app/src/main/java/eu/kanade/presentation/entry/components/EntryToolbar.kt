package eu.kanade.presentation.entry.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import eu.kanade.presentation.entry.DownloadAction
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Deselect
import mihon.icons.materialsymbols.rounded.Download
import mihon.icons.materialsymbols.rounded.FilterList
import mihon.icons.materialsymbols.rounded.SelectAll
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.AppBarActions
import mihon.presentation.kit.navigation.AppBarTitle
import mihon.presentation.kit.theme.ContentTone
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun EntryToolbar(
    title: String,
    hasFilters: Boolean,
    navigateUp: () -> Unit,
    onClickFilter: () -> Unit,
    onClickShare: (() -> Unit)?,
    onClickDownload: ((DownloadAction) -> Unit)?,
    bookmarkedDownloadsSupported: Boolean,
    onClickEditCategory: (() -> Unit)?,
    onClickEditDisplayName: (() -> Unit)?,
    onClickManageMerge: (() -> Unit)?,
    onClickRefresh: () -> Unit,
    onClickMigrate: (() -> Unit)?,
    onClickRelatedEntries: (() -> Unit)?,
    onClickEditNotes: () -> Unit,
    modifier: Modifier = Modifier,
    entryType: EntryType? = null,

    // For action mode
    actionModeCounter: Int,
    onCancelActionMode: () -> Unit,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,

    titleAlphaProvider: () -> Float,
    backgroundAlphaProvider: () -> Float,
) {
    val isActionMode = actionModeCounter > 0
    AppBar(
        titleContent = {
            if (isActionMode) {
                AppBarTitle(actionModeCounter.toString())
            } else {
                AppBarTitle(title, modifier = Modifier.alpha(titleAlphaProvider()))
            }
        },
        modifier = modifier,
        containerAlpha = backgroundAlphaProvider,
        navigateUp = navigateUp,
        actions = {
            var downloadExpanded by remember { mutableStateOf(false) }
            val filterTone = if (hasFilters) ContentTone.Active else ContentTone.Inherit
            AppBarActions(
                actions = buildList {
                    if (isActionMode) {
                        add(
                            AppBar.Action(
                                title = stringResource(MR.strings.action_select_all),
                                icon = MaterialSymbols.Rounded.SelectAll,
                                onClick = onSelectAll,
                            ),
                        )
                        add(
                            AppBar.Action(
                                title = stringResource(MR.strings.action_select_inverse),
                                icon = MaterialSymbols.Rounded.Deselect,
                                onClick = onInvertSelection,
                            ),
                        )
                        return@buildList
                    }
                    if (onClickDownload != null) {
                        add(
                            AppBar.Action(
                                title = stringResource(MR.strings.manga_download),
                                icon = MaterialSymbols.Rounded.Download,
                                onClick = { downloadExpanded = !downloadExpanded },
                                menu = {
                                    DownloadDropdownMenu(
                                        expanded = downloadExpanded,
                                        onDismissRequest = { downloadExpanded = false },
                                        onDownloadClicked = onClickDownload,
                                        bookmarkedDownloadsSupported = bookmarkedDownloadsSupported,
                                        presentation = entryType.entryTypePresentation(),
                                    )
                                },
                            ),
                        )
                    }
                    add(
                        AppBar.Action(
                            title = stringResource(MR.strings.action_filter),
                            icon = MaterialSymbols.Rounded.FilterList,
                            tone = filterTone,
                            onClick = onClickFilter,
                        ),
                    )
                    add(
                        AppBar.OverflowAction(
                            title = stringResource(MR.strings.action_webview_refresh),
                            onClick = onClickRefresh,
                        ),
                    )
                    if (onClickEditCategory != null) {
                        add(
                            AppBar.OverflowAction(
                                title = stringResource(MR.strings.action_edit_categories),
                                onClick = onClickEditCategory,
                            ),
                        )
                    }
                    if (onClickEditDisplayName != null) {
                        add(
                            AppBar.OverflowAction(
                                title = stringResource(MR.strings.action_set_display_name),
                                onClick = onClickEditDisplayName,
                            ),
                        )
                    }
                    if (onClickManageMerge != null) {
                        add(
                            AppBar.OverflowAction(
                                title = stringResource(MR.strings.action_manage_merge),
                                onClick = onClickManageMerge,
                            ),
                        )
                    }
                    if (onClickMigrate != null) {
                        add(
                            AppBar.OverflowAction(
                                title = stringResource(MR.strings.action_migrate),
                                onClick = onClickMigrate,
                            ),
                        )
                    }
                    if (onClickRelatedEntries != null) {
                        add(
                            AppBar.OverflowAction(
                                title = stringResource(MR.strings.related_entries),
                                onClick = onClickRelatedEntries,
                            ),
                        )
                    }
                    if (onClickShare != null) {
                        add(
                            AppBar.OverflowAction(
                                title = stringResource(MR.strings.action_share),
                                onClick = onClickShare,
                            ),
                        )
                    }
                    add(
                        AppBar.OverflowAction(
                            title = stringResource(MR.strings.action_notes),
                            onClick = onClickEditNotes,
                        ),
                    )
                },
            )
        },
        isActionMode = isActionMode,
        onCancelActionMode = onCancelActionMode,
    )
}
