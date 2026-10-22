package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Delete
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.dialog.ConfirmDialog
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.theme.KatariTheme
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
    Dialog(
        onDismissRequest = {},
        title = stringResource(MR.strings.pref_duplicate_detection),
        confirm = null,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = KatariTheme.spacing.s),
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LoadingIndicator()
            Text(text = stringResource(MR.strings.loading))
        }
    }
}
