package eu.kanade.tachiyomi.ui.browse.feed.manage

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.field.TextField
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Names a feed. Saving an empty name goes back to showing [presetName]. */
@Composable
internal fun RenameFeedDialog(
    currentTitle: String?,
    presetName: String,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by rememberSaveable { mutableStateOf(currentTitle.orEmpty()) }
    Dialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(MR.strings.browse_feed_rename_title),
        confirm = DialogAction(stringResource(MR.strings.action_save), onClick = { onConfirm(value) }),
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onClick = onDismissRequest),
    ) {
        TextField(
            value = value,
            onValueChange = { value = it },
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(MR.strings.name),
            placeholder = presetName,
            supportingText = stringResource(MR.strings.browse_feed_rename_hint, presetName),
        )
    }
}
