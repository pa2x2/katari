package eu.kanade.presentation.entry.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.entry.EntryTypePresentation
import eu.kanade.presentation.entry.entryTypePresentation
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mihon.icons.custommaterialsymbols.CustomMaterialSymbols
import mihon.icons.custommaterialsymbols.rounded.DonePrevious
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.BookmarkAdd
import mihon.icons.materialsymbols.rounded.BookmarkRemove
import mihon.icons.materialsymbols.rounded.Delete
import mihon.icons.materialsymbols.rounded.DoneAll
import mihon.icons.materialsymbols.rounded.Download
import mihon.icons.materialsymbols.rounded.RemoveDone
import mihon.icons.materialsymbols.rounded.Translate
import mihon.presentation.kit.menu.DropdownMenuItem
import mihon.presentation.kit.menu.OverflowMenu
import mihon.presentation.kit.navigation.SelectionAction
import mihon.presentation.kit.navigation.SelectionActionBar
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.time.Duration.Companion.seconds

@Composable
fun EntryBottomActionMenu(
    visible: Boolean,
    modifier: Modifier = Modifier,
    onBookmarkClicked: (() -> Unit)? = null,
    onRemoveBookmarkClicked: (() -> Unit)? = null,
    onMarkAsReadClicked: (() -> Unit)? = null,
    onMarkAsUnreadClicked: (() -> Unit)? = null,
    bookmarkLabel: StringResource = MR.strings.action_bookmark,
    removeBookmarkLabel: StringResource = MR.strings.action_remove_bookmark,
    markAsReadLabel: StringResource = MR.strings.action_mark_as_read,
    markAsUnreadLabel: StringResource = MR.strings.action_mark_as_unread,
    markPreviousAsReadLabel: StringResource = MR.strings.action_mark_previous_as_read,
    downloadPresentation: EntryTypePresentation = null.entryTypePresentation(),
    onMarkPreviousAsReadClicked: (() -> Unit)? = null,
    onDownloadClicked: (() -> Unit)? = null,
    onDeleteClicked: (() -> Unit)? = null,
    onTranslateClicked: (() -> Unit)? = null,
    onDeleteTranslationClicked: (() -> Unit)? = null,
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(expandFrom = Alignment.Bottom),
        exit = shrinkVertically(shrinkTowards = Alignment.Bottom),
    ) {
        val scope = rememberCoroutineScope()
        SelectionActionBar(modifier = modifier) {
            val haptic = LocalHapticFeedback.current
            val confirm = remember { mutableStateListOf(false, false, false, false, false, false, false, false) }
            var resetJob by remember { mutableStateOf<Job?>(null) }
            val onLongClickItem: (Int) -> Unit = { toConfirmIndex ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                confirm.indices.forEach { i -> confirm[i] = i == toConfirmIndex }
                resetJob?.cancel()
                resetJob = scope.launch {
                    delay(1.seconds)
                    if (isActive) confirm[toConfirmIndex] = false
                }
            }
            if (onBookmarkClicked != null) {
                SelectionAction(
                    title = stringResource(bookmarkLabel),
                    icon = MaterialSymbols.Rounded.BookmarkAdd,
                    revealLabel = confirm[0],
                    onLongClick = { onLongClickItem(0) },
                    onClick = onBookmarkClicked,
                )
            }
            if (onRemoveBookmarkClicked != null) {
                SelectionAction(
                    title = stringResource(removeBookmarkLabel),
                    icon = MaterialSymbols.Rounded.BookmarkRemove,
                    revealLabel = confirm[1],
                    onLongClick = { onLongClickItem(1) },
                    onClick = onRemoveBookmarkClicked,
                )
            }
            if (onMarkAsReadClicked != null) {
                SelectionAction(
                    title = stringResource(markAsReadLabel),
                    icon = MaterialSymbols.Rounded.DoneAll,
                    revealLabel = confirm[2],
                    onLongClick = { onLongClickItem(2) },
                    onClick = onMarkAsReadClicked,
                )
            }
            if (onMarkAsUnreadClicked != null) {
                SelectionAction(
                    title = stringResource(markAsUnreadLabel),
                    icon = MaterialSymbols.Rounded.RemoveDone,
                    revealLabel = confirm[3],
                    onLongClick = { onLongClickItem(3) },
                    onClick = onMarkAsUnreadClicked,
                )
            }
            if (onMarkPreviousAsReadClicked != null) {
                SelectionAction(
                    title = stringResource(markPreviousAsReadLabel),
                    icon = CustomMaterialSymbols.Rounded.DonePrevious,
                    revealLabel = confirm[4],
                    onLongClick = { onLongClickItem(4) },
                    onClick = onMarkPreviousAsReadClicked,
                )
            }
            if (onDownloadClicked != null) {
                SelectionAction(
                    title = stringResource(MR.strings.action_download),
                    icon = MaterialSymbols.Rounded.Download,
                    revealLabel = confirm[5],
                    onLongClick = { onLongClickItem(5) },
                    onClick = onDownloadClicked,
                )
            }
            if (onTranslateClicked != null) {
                SelectionAction(
                    title = stringResource(MR.strings.action_translate),
                    icon = MaterialSymbols.Rounded.Translate,
                    revealLabel = confirm[7],
                    onLongClick = { onLongClickItem(7) },
                    onClick = onTranslateClicked,
                )
            }
            if (onDeleteClicked != null) {
                SelectionAction(
                    title = stringResource(MR.strings.action_delete),
                    icon = MaterialSymbols.Rounded.Delete,
                    revealLabel = confirm[6],
                    onLongClick = { onLongClickItem(6) },
                    onClick = onDeleteClicked,
                )
            }
            if (onDeleteTranslationClicked != null) {
                OverflowMenu { close ->
                    DropdownMenuItem(
                        label = stringResource(MR.strings.action_delete_translation),
                        onClick = {
                            close()
                            onDeleteTranslationClicked()
                        },
                    )
                }
            }
        }
    }
}
