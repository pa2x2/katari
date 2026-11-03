package eu.kanade.presentation.entry.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.field.TextField
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Edits the name the entry is shown under; Reset confirms an empty name, which falls back to the source's title. */
@Composable
fun EditDisplayNameDialog(
    initialValue: String,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    var value by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(
            TextFieldValue(
                text = initialValue,
                selection = TextRange(initialValue.length),
            ),
        )
    }

    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(MR.strings.action_set_display_name),
        confirm = DialogAction(stringResource(MR.strings.action_save), onClick = { onConfirm(value.text) }),
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onDismissRequest),
        neutral = DialogAction(stringResource(MR.strings.action_reset), onClick = { onConfirm("") }),
    ) {
        TextField(
            value = value,
            onValueChange = { value = it },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
        )
    }
}
