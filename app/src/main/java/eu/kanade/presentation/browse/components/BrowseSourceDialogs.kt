package eu.kanade.presentation.browse.components

import androidx.compose.runtime.Composable
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Delete
import mihon.presentation.kit.dialog.ConfirmDialog
import mihon.presentation.kit.dialog.ProgressDialog
import tachiyomi.domain.entry.model.Entry
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun RemoveMangaDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    mangaToRemove: Entry,
) {
    ConfirmDialog(
        title = stringResource(MR.strings.are_you_sure),
        text = stringResource(MR.strings.remove_manga, mangaToRemove.title),
        confirmLabel = stringResource(MR.strings.action_remove),
        onConfirm = {
            onDismissRequest()
            onConfirm()
        },
        onDismissRequest = onDismissRequest,
        destructive = true,
        icon = MaterialSymbols.Rounded.Delete,
    )
}

@Composable
fun DuplicateDetectionLoadingDialog() {
    ProgressDialog(
        title = stringResource(MR.strings.pref_duplicate_detection),
        message = stringResource(MR.strings.duplicate_detection_checking),
    )
}
