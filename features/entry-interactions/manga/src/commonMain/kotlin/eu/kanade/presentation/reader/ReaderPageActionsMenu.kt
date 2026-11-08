package eu.kanade.presentation.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.ContentCopy
import app.katari.icons.materialsymbols.rounded.Photo
import app.katari.icons.materialsymbols.rounded.Save
import app.katari.icons.materialsymbols.rounded.Share
import app.katari.presentation.kit.menu.ContextMenu
import app.katari.presentation.kit.menu.DropdownMenuItem
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The actions on a right-clicked page, as a menu at [windowPosition], where the click landed in the window. The same
 * actions as [ReaderPageActionsDialog]; [onCopy], [onShare] and [onSave] are null where the platform can't do them.
 */
@Composable
fun ReaderPageActionsMenu(
    windowPosition: Offset,
    onDismissRequest: () -> Unit,
    onSetAsCover: () -> Unit,
    onCopy: (() -> Unit)?,
    onShare: (() -> Unit)?,
    onSave: (() -> Unit)?,
) {
    var origin by remember { mutableStateOf<LayoutCoordinates?>(null) }
    // Choosing Set as cover closes the menu, and the actions stay open for the confirmation it asks for
    var confirmingCover by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxSize().onGloballyPositioned { origin = it }) {
        val position = origin?.takeIf { it.isAttached }?.windowToLocal(windowPosition)
        if (position != null && !confirmingCover) {
            ContextMenu(position = position, onDismissRequest = { if (!confirmingCover) onDismissRequest() }) {
                if (onSave != null) {
                    DropdownMenuItem(
                        label = stringResource(MR.strings.action_save),
                        icon = MaterialSymbols.Rounded.Save,
                        onClick = onSave,
                    )
                }
                if (onCopy != null) {
                    DropdownMenuItem(
                        label = stringResource(MR.strings.action_copy_to_clipboard),
                        icon = MaterialSymbols.Rounded.ContentCopy,
                        onClick = onCopy,
                    )
                }
                if (onShare != null) {
                    DropdownMenuItem(
                        label = stringResource(MR.strings.action_share),
                        icon = MaterialSymbols.Rounded.Share,
                        onClick = onShare,
                    )
                }
                DropdownMenuItem(
                    label = stringResource(MR.strings.set_as_cover),
                    icon = MaterialSymbols.Rounded.Photo,
                    onClick = { confirmingCover = true },
                )
            }
        }
    }
    if (confirmingCover) {
        SetCoverDialog(
            onConfirm = {
                onSetAsCover()
                onDismissRequest()
            },
            onDismiss = onDismissRequest,
        )
    }
}
