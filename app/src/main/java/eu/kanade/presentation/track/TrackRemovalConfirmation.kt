package eu.kanade.presentation.track

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.list.RowGroup
import mihon.presentation.kit.sheet.SheetActions
import mihon.presentation.kit.sheet.SheetHeader
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * A page of the tracking sheet that confirms removing something, such as a tracker or a reading date. [options] adds
 * a choice in a card under the message, such as also deleting the entry on the tracker's site.
 */
@Composable
fun TrackRemovalConfirmation(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    options: (@Composable () -> Unit)? = null,
) {
    Column {
        SheetHeader(title = title)
        Column(
            modifier = Modifier.padding(vertical = KatariTheme.spacing.m),
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        ) {
            Text(text = text, modifier = Modifier.padding(horizontal = KatariTheme.spacing.rowInset))
            options?.let { RowGroup(rows = listOf(it)) }
        }
        SheetActions(
            confirm = DialogAction(confirmLabel, onConfirm, destructive = true),
            dismiss = DialogAction(stringResource(MR.strings.action_cancel), onDismissRequest),
        )
    }
}
