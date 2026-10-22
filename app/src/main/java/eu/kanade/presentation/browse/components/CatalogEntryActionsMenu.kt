package eu.kanade.presentation.browse.components

import androidx.compose.runtime.Composable
import eu.kanade.presentation.entry.components.EntryActionIcons
import mihon.presentation.kit.menu.DropdownMenu
import mihon.presentation.kit.menu.DropdownMenuItem
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * What a long press on a catalogue entry offers, as a menu at the entry: the library action, merging it into a library
 * entry, and opening it. The caller composes it inside the entry's cell only while it is open, so it anchors there.
 */
@Composable
fun CatalogEntryActionsMenu(
    favorite: Boolean,
    onDismissRequest: () -> Unit,
    onLibraryAction: () -> Unit,
    onMergeIntoLibrary: () -> Unit,
    onOpen: () -> Unit,
) {
    DropdownMenu(expanded = true, onDismissRequest = onDismissRequest) {
        DropdownMenuItem(
            label = stringResource(if (favorite) MR.strings.remove_from_library else MR.strings.add_to_library),
            onClick = onLibraryAction,
            icon = EntryActionIcons.library(favorite),
        )
        DropdownMenuItem(
            label = stringResource(MR.strings.action_merge_into_library),
            onClick = onMergeIntoLibrary,
            icon = EntryActionIcons.merge,
        )
        DropdownMenuItem(
            label = stringResource(MR.strings.action_open),
            onClick = onOpen,
            icon = EntryActionIcons.openFullEntry,
        )
    }
}
