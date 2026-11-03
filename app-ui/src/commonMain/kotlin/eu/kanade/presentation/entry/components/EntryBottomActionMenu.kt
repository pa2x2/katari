package eu.kanade.presentation.entry.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.icerock.moko.resources.StringResource
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
import mihon.presentation.kit.navigation.SelectionActionBar
import mihon.presentation.kit.selection.ItemAction
import mihon.presentation.kit.selection.SelectionActions
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The bar of actions on selected chapters or episodes, as an entry's chapter list and Updates show it. */
@Composable
fun EntryBottomActionMenu(
    visible: Boolean,
    actions: SelectionActions,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(expandFrom = Alignment.Bottom),
        exit = shrinkVertically(shrinkTowards = Alignment.Bottom),
    ) {
        SelectionActionBar(actions = actions, modifier = modifier)
    }
}

/** The actions on selected chapters or episodes, for their bar and for their rows' right-click menus. */
@Composable
fun chapterSelectionActions(
    onBookmarkClicked: (() -> Unit)? = null,
    onRemoveBookmarkClicked: (() -> Unit)? = null,
    onMarkAsReadClicked: (() -> Unit)? = null,
    onMarkAsUnreadClicked: (() -> Unit)? = null,
    bookmarkLabel: StringResource = MR.strings.action_bookmark,
    removeBookmarkLabel: StringResource = MR.strings.action_remove_bookmark,
    markAsReadLabel: StringResource = MR.strings.action_mark_as_read,
    markAsUnreadLabel: StringResource = MR.strings.action_mark_as_unread,
    markPreviousAsReadLabel: StringResource = MR.strings.action_mark_previous_as_read,
    onMarkPreviousAsReadClicked: (() -> Unit)? = null,
    onDownloadClicked: (() -> Unit)? = null,
    onDeleteClicked: (() -> Unit)? = null,
    onTranslateClicked: (() -> Unit)? = null,
    onDeleteTranslationClicked: (() -> Unit)? = null,
): SelectionActions {
    val actions = listOfNotNull(
        onBookmarkClicked?.let {
            ItemAction(stringResource(bookmarkLabel), MaterialSymbols.Rounded.BookmarkAdd, it)
        },
        onRemoveBookmarkClicked?.let {
            ItemAction(stringResource(removeBookmarkLabel), MaterialSymbols.Rounded.BookmarkRemove, it)
        },
        onMarkAsReadClicked?.let {
            ItemAction(stringResource(markAsReadLabel), MaterialSymbols.Rounded.DoneAll, it)
        },
        onMarkAsUnreadClicked?.let {
            ItemAction(stringResource(markAsUnreadLabel), MaterialSymbols.Rounded.RemoveDone, it)
        },
        onMarkPreviousAsReadClicked?.let {
            ItemAction(stringResource(markPreviousAsReadLabel), CustomMaterialSymbols.Rounded.DonePrevious, it)
        },
        onDownloadClicked?.let {
            ItemAction(stringResource(MR.strings.action_download), MaterialSymbols.Rounded.Download, it)
        },
        onTranslateClicked?.let {
            ItemAction(stringResource(MR.strings.action_translate), MaterialSymbols.Rounded.Translate, it)
        },
        onDeleteClicked?.let {
            ItemAction(stringResource(MR.strings.action_delete), MaterialSymbols.Rounded.Delete, it)
        },
    )
    val overflow = listOfNotNull(
        onDeleteTranslationClicked?.let {
            ItemAction(stringResource(MR.strings.action_delete_translation), MaterialSymbols.Rounded.Delete, it)
        },
    )
    return SelectionActions(actions = actions, overflow = listOf(overflow))
}
