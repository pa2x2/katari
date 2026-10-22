package eu.kanade.presentation.browse.components

import androidx.compose.runtime.Composable
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.sheet.SheetActions
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Opening the full entry, merging it and the library action, under an entry's preview. The library action is named as
 * on the entry screen: "In library" while the entry is in it.
 */
@Composable
internal fun BrowseEntryPreviewBottomBar(
    favorite: Boolean,
    onOpenEntry: () -> Unit,
    onLibraryAction: () -> Unit,
    onMergeAction: () -> Unit,
) {
    SheetActions(
        confirm = DialogAction(
            label = stringResource(if (favorite) MR.strings.in_library else MR.strings.add_to_library),
            onClick = onLibraryAction,
        ),
        dismiss = DialogAction(stringResource(MR.strings.action_add_to_merge), onMergeAction),
        neutral = DialogAction(stringResource(MR.strings.action_open), onOpenEntry),
    )
}
