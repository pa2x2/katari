package eu.kanade.presentation.library.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.entry.DownloadAction
import eu.kanade.presentation.entry.EntryTypePresentation
import eu.kanade.presentation.entry.components.DownloadDropdownMenu
import eu.kanade.presentation.entry.components.EntryActionIcons
import eu.kanade.presentation.entry.entryTypePresentation
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.Label
import mihon.icons.materialsymbols.rounded.DoneAll
import mihon.icons.materialsymbols.rounded.Download
import mihon.icons.materialsymbols.rounded.Keep
import mihon.icons.materialsymbols.rounded.MoreVert
import mihon.icons.materialsymbols.rounded.RemoveDone
import mihon.icons.materialsymbols.roundedfilled.Keep
import mihon.presentation.kit.menu.DropdownMenu
import mihon.presentation.kit.menu.DropdownMenuItem
import mihon.presentation.kit.navigation.SelectionAction
import mihon.presentation.kit.navigation.SelectionActionBar
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun LibraryBottomActionMenu(
    visible: Boolean,
    onMergeClicked: (() -> Unit)?,
    onPinClicked: (() -> Unit)?,
    pinSelection: Boolean,
    onChangeCategoryClicked: (() -> Unit)?,
    onMarkAsReadClicked: (() -> Unit)?,
    onMarkAsUnreadClicked: (() -> Unit)?,
    onDownloadClicked: ((DownloadAction) -> Unit)?,
    onDeleteClicked: (() -> Unit)?,
    onMigrateClicked: (() -> Unit)?,
    onMoveToProfileClicked: (() -> Unit)?,
    onCheckForUpdatesClicked: () -> Unit,
    onUpdateModeClicked: () -> Unit,
    modifier: Modifier = Modifier,
    markAsReadLabel: StringResource = MR.strings.action_mark_as_read,
    markAsUnreadLabel: StringResource = MR.strings.action_mark_as_unread,
    downloadPresentation: EntryTypePresentation = null.entryTypePresentation(),
    bookmarkedDownloadsSupported: Boolean,
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(animationSpec = tween(delayMillis = 300)),
        exit = shrinkVertically(animationSpec = tween()),
    ) {
        SelectionActionBar(modifier = modifier) {
            if (onChangeCategoryClicked != null) {
                SelectionAction(
                    title = stringResource(MR.strings.action_move_category),
                    icon = MaterialSymbols.AutoMirroredRounded.Label,
                    onLongClick = {},
                    showLabel = true,
                    onClick = onChangeCategoryClicked,
                )
            }
            if (onPinClicked != null) {
                SelectionAction(
                    title = stringResource(if (pinSelection) MR.strings.action_pin else MR.strings.action_unpin),
                    icon = if (pinSelection) MaterialSymbols.Rounded.Keep else MaterialSymbols.RoundedFilled.Keep,
                    onLongClick = {},
                    showLabel = true,
                    onClick = onPinClicked,
                )
            }
            if (onMergeClicked != null) {
                SelectionAction(
                    title = stringResource(MR.strings.action_merge),
                    icon = EntryActionIcons.merge,
                    onLongClick = {},
                    showLabel = true,
                    onClick = onMergeClicked,
                )
            }
            if (onMarkAsReadClicked != null) {
                SelectionAction(
                    title = stringResource(markAsReadLabel),
                    icon = MaterialSymbols.Rounded.DoneAll,
                    onLongClick = {},
                    showLabel = true,
                    onClick = onMarkAsReadClicked,
                )
            }
            if (onMarkAsUnreadClicked != null) {
                SelectionAction(
                    title = stringResource(markAsUnreadLabel),
                    icon = MaterialSymbols.Rounded.RemoveDone,
                    onLongClick = {},
                    showLabel = true,
                    onClick = onMarkAsUnreadClicked,
                )
            }
            if (onDownloadClicked != null) {
                var downloadExpanded by remember { mutableStateOf(false) }
                SelectionAction(
                    title = stringResource(MR.strings.action_download),
                    icon = MaterialSymbols.Rounded.Download,
                    onLongClick = {},
                    showLabel = true,
                    onClick = { downloadExpanded = !downloadExpanded },
                    menu = {
                        DownloadDropdownMenu(
                            expanded = downloadExpanded,
                            onDismissRequest = { downloadExpanded = false },
                            onDownloadClicked = onDownloadClicked,
                            bookmarkedDownloadsSupported = bookmarkedDownloadsSupported,
                            presentation = downloadPresentation,
                        )
                    },
                )
            }
            var overflowMenuOpen by remember { mutableStateOf(false) }
            SelectionAction(
                title = stringResource(MR.strings.label_more),
                icon = MaterialSymbols.Rounded.MoreVert,
                onLongClick = {},
                showLabel = true,
                onClick = { overflowMenuOpen = true },
                menu = {
                    DropdownMenu(
                        expanded = overflowMenuOpen,
                        onDismissRequest = { overflowMenuOpen = false },
                    ) {
                        DropdownMenuItem(
                            label = stringResource(MR.strings.action_check_for_updates),
                            onClick = {
                                overflowMenuOpen = false
                                onCheckForUpdatesClicked()
                            },
                        )
                        DropdownMenuItem(
                            label = stringResource(MR.strings.action_update_mode),
                            onClick = {
                                overflowMenuOpen = false
                                onUpdateModeClicked()
                            },
                        )
                        if (onMoveToProfileClicked != null) {
                            DropdownMenuItem(
                                label = stringResource(MR.strings.move_entries_action),
                                onClick = onMoveToProfileClicked,
                            )
                        }
                        if (onMigrateClicked != null) {
                            DropdownMenuItem(
                                label = stringResource(MR.strings.migrate),
                                onClick = onMigrateClicked,
                            )
                        }
                        if (onDeleteClicked != null) {
                            DropdownMenuItem(
                                label = stringResource(MR.strings.action_delete),
                                onClick = onDeleteClicked,
                            )
                        }
                    }
                },
            )
        }
    }
}
