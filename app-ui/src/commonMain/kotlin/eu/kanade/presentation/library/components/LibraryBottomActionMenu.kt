package eu.kanade.presentation.library.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.entry.DownloadAction
import eu.kanade.presentation.entry.EntryTypePresentation
import eu.kanade.presentation.entry.components.EntryActionIcons
import eu.kanade.presentation.entry.components.downloadChoices
import eu.kanade.presentation.entry.entryTypePresentation
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.Label
import mihon.icons.materialsymbols.rounded.Delete
import mihon.icons.materialsymbols.rounded.DoneAll
import mihon.icons.materialsymbols.rounded.Download
import mihon.icons.materialsymbols.rounded.Keep
import mihon.icons.materialsymbols.rounded.Person
import mihon.icons.materialsymbols.rounded.Refresh
import mihon.icons.materialsymbols.rounded.RemoveDone
import mihon.icons.materialsymbols.rounded.SwapHoriz
import mihon.icons.materialsymbols.rounded.Update
import mihon.icons.materialsymbols.roundedfilled.Keep
import mihon.presentation.kit.navigation.SelectionActionBar
import mihon.presentation.kit.navigation.SelectionActionLabels
import mihon.presentation.kit.selection.ItemAction
import mihon.presentation.kit.selection.SelectionActions
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun LibraryBottomActionMenu(
    visible: Boolean,
    actions: SelectionActions,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(animationSpec = tween(delayMillis = 300)),
        exit = shrinkVertically(animationSpec = tween()),
    ) {
        SelectionActionBar(actions = actions, modifier = modifier, labels = SelectionActionLabels.Shown)
    }
}

/** The actions on the library's selection, for its bar and for its covers' right-click menus. */
@Composable
fun librarySelectionActions(
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
    onCheckForUpdatesClicked: (() -> Unit)?,
    onUpdateModeClicked: () -> Unit,
    markAsReadLabel: StringResource = MR.strings.action_mark_as_read,
    markAsUnreadLabel: StringResource = MR.strings.action_mark_as_unread,
    downloadPresentation: EntryTypePresentation = null.entryTypePresentation(),
    bookmarkedDownloadsSupported: Boolean,
): SelectionActions {
    val actions = buildList {
        if (onChangeCategoryClicked != null) {
            add(
                ItemAction(
                    label = stringResource(MR.strings.action_move_category),
                    icon = MaterialSymbols.AutoMirroredRounded.Label,
                    onClick = onChangeCategoryClicked,
                ),
            )
        }
        if (onPinClicked != null) {
            add(
                ItemAction(
                    label = stringResource(if (pinSelection) MR.strings.action_pin else MR.strings.action_unpin),
                    icon = if (pinSelection) MaterialSymbols.Rounded.Keep else MaterialSymbols.RoundedFilled.Keep,
                    onClick = onPinClicked,
                ),
            )
        }
        if (onMergeClicked != null) {
            add(
                ItemAction(
                    label = stringResource(MR.strings.action_merge),
                    icon = EntryActionIcons.merge,
                    onClick = onMergeClicked,
                ),
            )
        }
        if (onMarkAsReadClicked != null) {
            add(
                ItemAction(
                    label = stringResource(markAsReadLabel),
                    icon = MaterialSymbols.Rounded.DoneAll,
                    onClick = onMarkAsReadClicked,
                ),
            )
        }
        if (onMarkAsUnreadClicked != null) {
            add(
                ItemAction(
                    label = stringResource(markAsUnreadLabel),
                    icon = MaterialSymbols.Rounded.RemoveDone,
                    onClick = onMarkAsUnreadClicked,
                ),
            )
        }
        if (onDownloadClicked != null) {
            add(
                ItemAction(
                    label = stringResource(MR.strings.action_download),
                    icon = MaterialSymbols.Rounded.Download,
                    choices = downloadChoices(onDownloadClicked, bookmarkedDownloadsSupported, downloadPresentation),
                ),
            )
        }
    }
    val updates = buildList {
        if (onCheckForUpdatesClicked != null) {
            add(
                ItemAction(
                    label = stringResource(MR.strings.action_check_for_updates),
                    icon = MaterialSymbols.Rounded.Refresh,
                    onClick = onCheckForUpdatesClicked,
                ),
            )
        }
        add(
            ItemAction(
                label = stringResource(MR.strings.action_update_mode),
                icon = MaterialSymbols.Rounded.Update,
                onClick = onUpdateModeClicked,
            ),
        )
    }
    val moves = buildList {
        if (onMoveToProfileClicked != null) {
            add(
                ItemAction(
                    label = stringResource(MR.strings.move_entries_action),
                    icon = MaterialSymbols.Rounded.Person,
                    onClick = onMoveToProfileClicked,
                ),
            )
        }
        if (onMigrateClicked != null) {
            add(
                ItemAction(
                    label = stringResource(MR.strings.migrate),
                    icon = MaterialSymbols.Rounded.SwapHoriz,
                    onClick = onMigrateClicked,
                ),
            )
        }
    }
    val removal = listOfNotNull(
        onDeleteClicked?.let {
            ItemAction(
                label = stringResource(MR.strings.action_delete),
                icon = MaterialSymbols.Rounded.Delete,
                onClick = it,
            )
        },
    )
    return SelectionActions(actions = actions, overflow = listOf(updates, moves, removal))
}
